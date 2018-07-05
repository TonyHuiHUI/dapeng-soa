package com.github.dapeng.json;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaHeaderSerializer;
import com.github.dapeng.core.helper.SoaHeaderHelper;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static com.github.dapeng.core.enums.CodecProtocol.CompressedBinary;
import static com.github.dapeng.core.helper.IPUtils.transferIp;
import static com.github.dapeng.json.JsonUtils.isCollectionKind;
import static com.github.dapeng.json.JsonUtils.isMultiElementKind;
import static com.github.dapeng.json.JsonUtils.isValidMapKeyType;
import static com.github.dapeng.util.MetaDataUtil.*;

/**
 * format:
 * url:http://xxx/api/callService?serviceName=xxx&version=xx&method=xx
 * post body:
 * {
 * "header":{},
 * "body":{
 * ${structName}:{}
 * }
 * }
 * <p>
 * InvocationContext and SoaHeader should be ready before
 */
enum ParsePhase {
    INIT, HEADER_BEGIN, HEADER, HEADER_END, BODY_BEGIN, BODY, BODY_END
}

/**
 * @author ever
 */
public class JsonReader implements JsonCallback {
    private final Logger logger = LoggerFactory.getLogger(JsonReader.class);

    private final Service service;
    private final Method method;
    private final String version;
    private final Struct struct;
    private final ByteBuf requestByteBuf;

    private final InvocationContext invocationCtx = InvocationContextImpl.Factory.currentInstance();


    /**
     * 压缩二进制编码下,集合的默认长度(占3字节)
     */
    private final TProtocol oproto;
    private ParsePhase parsePhase = ParsePhase.INIT;

    /**
     * 用于保存当前处理节点的信息
     */
    class StackNode {
        final DataType dataType;
        /**
         * byteBuf position after this node created
         */
        final int byteBufPosition;

        /**
         * byteBuf position before this node created
         */
        final int byteBufPositionBefore;

        /**
         * struct if dataType.kind==STRUCT
         */
        final Struct struct;

        /**
         * the field name
         */
        final String fieldName;

        /**
         * if datatype is struct, all fields parsed will be add to this set
         */
        final Set<String> fields4Struct = new HashSet<>(32);

        /**
         * if dataType is a Collection(such as LIST, MAP, SET etc), elCount represents the size of the Collection.
         */
        private int elCount = 0;

        StackNode(final DataType dataType, final int byteBufPosition, int byteBufPositionBefore, final Struct struct, String fieldName) {
            this.dataType = dataType;
            this.byteBufPosition = byteBufPosition;
            this.byteBufPositionBefore = byteBufPositionBefore;
            this.struct = struct;
            this.fieldName = fieldName;
        }

        void increaseElement() {
            elCount++;
        }
    }

    //当前处理数据节点
    StackNode current;
    String currentHeaderName;
    //onStartField的时候, 记录是否找到该Field. 如果没找到,那么需要skip这个field
    boolean foundField = true;
    /**
     * todo
     * 从第一个多余字段开始后,到该多余字段结束, 所解析到的字段的计数值.
     * 用于在多余字段是复杂结构类型的情况下, 忽略该复杂结构内嵌套的所有多余字段
     * 例如:
     * {
     * createdOrderRequest: {
     * "buyerId":2,
     * "sellerId":3,
     * "uselessField1":{
     * "innerField1":"a",
     * "innerField2":"b"
     * },
     * "uselessField2":[{
     * "innerField3":"c",
     * "innerField4":"d"
     * },
     * {
     * "innerField3":"c",
     * "innerField4":"d"
     * }]
     * }
     * }
     * 当uselessField1跟uselessField2是多余字段时,我们要确保其内部所有字段都
     * 给skip掉.
     */
    int skipFieldsStack = 0;
    //记录是否是null. 前端在处理optional字段的时候, 可能会传入一个null,见单元测试
    boolean foundNull = true;

    /**
     * @param service
     * @param method
     * @param version
     * @param struct
     * @param oproto
     */
    JsonReader(Service service, Method method, String version, Struct struct, ByteBuf requestByteBuf, TProtocol oproto) {
        this.service = service;
        this.method = method;
        this.version = version;
        this.struct = struct;
        this.oproto = oproto;
        this.requestByteBuf = requestByteBuf;
    }


    /*  {  a:, b:, c: { ... }, d: [ { }, { } ]  }
     *
     *  init                -- [], topStruct
     *  onStartObject
     *    onStartField a    -- [topStruct], DataType a
     *    onEndField a
     *    ...
     *    onStartField c    -- [topStruct], StructC
     *      onStartObject
     *          onStartField
     *          onEndField
     *          ...
     *      onEndObject     -- [], topStruct
     *    onEndField c
     *
     *    onStartField d
     *      onStartArray    -- [topStruct] structD
     *          onStartObject
     *          onEndObject
     *      onEndArray      -- []
     *    onEndField d
     */
    Stack<StackNode> history = new Stack<>();

    @Override
    public void onStartObject() throws TException {
        switch (parsePhase) {
            case INIT:
                break;
            case HEADER_BEGIN:
                parsePhase = ParsePhase.HEADER;
                break;
            case HEADER:
                logAndThrowTException();
            case HEADER_END:
                break;
            case BODY_BEGIN:
                new SoaHeaderSerializer().write(SoaHeaderHelper.buildHeader(service.namespace + "." + service.name, version, method.name), new TBinaryProtocol(oproto.getTransport()));

                //初始化当前数据节点
                DataType initDataType = new DataType();
                initDataType.setKind(DataType.KIND.STRUCT);
                initDataType.qualifiedName = struct.name;
                current = new StackNode(initDataType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), struct, struct.name);

                oproto.writeStructBegin(new TStruct(current.struct.name));

                parsePhase = ParsePhase.BODY;
                break;
            case BODY:
                if (!foundField) {
                    skipFieldsStack++;
                    return;
                }

                assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

                if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                    peek().increaseElement();
                    //集合套集合的变态处理方式
                    current = new StackNode(peek().dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), current.struct, current.struct == null ? null : current.struct.name);
                }
                switch (current.dataType.kind) {
                    case STRUCT:
                        Struct struct = current.struct;//findStruct(current.dataType.qualifiedName, service);
                        if (struct == null) {
                            logger.error("struct not found");
                            logAndThrowTException();
                        }
                        oproto.writeStructBegin(new TStruct(struct.name));
                        break;
                    case MAP:
                        assert isValidMapKeyType(current.dataType.keyType.kind);
                        writeMapBegin(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), 0);
                        break;
                    default:
                        logAndThrowTException();
                }
                break;
            default:
                logAndThrowTException();
        }
    }

    @Override
    public void onEndObject() throws TException {
        switch (parsePhase) {
            case HEADER_BEGIN:
                logAndThrowTException();
                break;
            case HEADER:
                parsePhase = ParsePhase.HEADER_END;
                break;
            case HEADER_END:
                logAndThrowTException();
                break;
            case BODY_BEGIN:
                logAndThrowTException();
                break;
            case BODY:
                if (!foundField) {
                    skipFieldsStack--;
                    return;
                } else {
                    assert skipFieldsStack == 0;
                }

                assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

                switch (current.dataType.kind) {
                    case STRUCT:
                        validateStruct(current);

                        oproto.writeFieldStop();
                        oproto.writeStructEnd();
                        if (current.struct.name.equals(struct.name)) {
                            parsePhase = ParsePhase.BODY_END;
                        }
                        break;
                    case MAP:
                        oproto.writeMapEnd();

                        reWriteByteBuf();
                        break;
                    default:
                        logAndThrowTException();
                }
                break;
            case BODY_END:
                break;
            default:
                logAndThrowTException();
        }
    }

    private void validateStruct(StackNode current) throws TException {
        /**
         * 不在该Struct必填字段列表的字段列表
         */
        List<Field> mandatoryMissFileds = current.struct.fields.stream()
                .filter(field -> !field.isOptional())
                .filter(field -> !current.fields4Struct.contains(field.name))
                .collect(Collectors.toList());
        if (!mandatoryMissFileds.isEmpty()) {
            String fieldName = current.fieldName;
            String struct = current.struct.name;
            TException ex = new TException("JsonError, please check:"
                    + struct + "." + fieldName
                    + ", struct mandatory fields missing:"
                    + mandatoryMissFileds.stream().map(field -> field.name + ", ").collect(Collectors.toList()));
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * 由于目前拿不到集合的元素个数, 暂时设置为0个
     *
     * @throws TException
     */
    @Override
    public void onStartArray() throws TException {
        if (parsePhase != ParsePhase.BODY || !foundField) {
            if (!foundField) {
                skipFieldsStack++;
            }
            return;
        }

        assert isCollectionKind(current.dataType.kind);

        if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
            peek().increaseElement();
            //集合套集合的变态处理方式
            current = new StackNode(peek().dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), current.struct, current.struct == null ? null : current.struct.name);
        }

        switch (current.dataType.kind) {
            case LIST:
            case SET:
                writeCollectionBegin(dataType2Byte(current.dataType.valueType), 0);
                break;
            default:
                logAndThrowTException();
        }

        Struct nextStruct = findStruct(current.dataType.valueType.qualifiedName, service);
        stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), nextStruct, nextStruct == null ? "" : nextStruct.name));
    }

    @Override
    public void onEndArray() throws TException {
        if (parsePhase != ParsePhase.BODY || !foundField) {
            if (!foundField) {
                skipFieldsStack--;
            }
            return;
        }

        pop();

        assert isCollectionKind(current.dataType.kind);

        switch (current.dataType.kind) {
            case LIST:
                oproto.writeListEnd();
                reWriteByteBuf();
                break;
            case SET:
                oproto.writeSetEnd();
                reWriteByteBuf();
                break;
            default:
                //do nothing
        }
    }

    @Override
    public void onStartField(String name) throws TException {
        switch (parsePhase) {
            case INIT:
                if ("header".equals(name)) {
                    parsePhase = ParsePhase.HEADER_BEGIN;
                } else if ("body".equals(name)) {
                    parsePhase = ParsePhase.BODY_BEGIN;
                } else {
                    logger.debug("skip field(" + name + ")@pase:" + parsePhase);
                }
                break;
            case HEADER:
                currentHeaderName = name;
                break;
            case HEADER_END:
                if ("body".equals(name)) {
                    parsePhase = ParsePhase.BODY_BEGIN;
                } else {
                    logger.debug("skip field(" + name + ")@pase:" + parsePhase);
                }
                break;
            case BODY:
                if (!foundField) {
                    return;
                }

                if (current.dataType.kind == DataType.KIND.MAP) {
                    assert isValidMapKeyType(current.dataType.keyType.kind);
                    stackNew(new StackNode(current.dataType.keyType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), null, name));
                    // key有可能是String, 也有可能是Int
                    if (current.dataType.kind == DataType.KIND.STRING) {
                        oproto.writeString(name);
                    } else {
                        writeIntField(name, current.dataType.kind);
                    }
                    pop();
                    stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), findStruct(current.dataType.valueType.qualifiedName, service), name));
                } else {
                    // reset field status
                    foundNull = false;
                    foundField = true;
                    Field field = findField(name, current.struct);
                    if (field == null) {
                        foundField = false;
                        logger.debug("field(" + name + ") not found. just skip");
                        return;
                    }

                    if (current.dataType.kind == DataType.KIND.STRUCT) {
                        current.fields4Struct.add(name);
                    }

                    int byteBufPositionBefore = requestByteBuf.writerIndex();
                    oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
                    stackNew(new StackNode(field.dataType, requestByteBuf.writerIndex(), byteBufPositionBefore, findStruct(field.dataType.qualifiedName, service), name));
                }
                break;
            case BODY_END:
                logger.debug("skip field(" + name + ")@pase:" + parsePhase);
                break;
            default:
                logAndThrowTException();
        }

    }

    private void writeIntField(String value, DataType.KIND kind) throws TException {
        switch (kind) {
            case SHORT:
                oproto.writeI16(Short.valueOf(value));
                break;
            case INTEGER:
                oproto.writeI32(Integer.valueOf(value));
                break;
            case LONG:
                oproto.writeI64(Long.valueOf(value));
                break;
            default:
                logAndThrowTException();
        }
    }

    @Override
    public void onEndField() throws TException {
        if (parsePhase != ParsePhase.BODY || !foundField) {
            // reset the flag
            if (skipFieldsStack == 0) {
                foundField = true;
            }
            return;
        }

        String fieldName = current.fieldName;
        pop();

        if (foundNull) {
            if (current.dataType.kind == DataType.KIND.STRUCT) {
                current.fields4Struct.remove(fieldName);
            }
        }
        if (current.dataType.kind != DataType.KIND.MAP && !foundNull) {
            oproto.writeFieldEnd();
        }

        foundNull = false;
    }

    @Override
    public void onBoolean(boolean value) throws TException {
        switch (parsePhase) {
            case HEADER:
                logger.debug("skip boolean(" + value + ")@pase:" + parsePhase + " field:" + current.fieldName);
                break;
            case BODY:
                if (!foundField) {
                    return;
                }
                if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                    peek().increaseElement();
                }

                oproto.writeBool(value);
                break;
            default:
                logger.debug("skip boolean(" + value + ")@pase:" + parsePhase + " for field:" + current.fieldName);
        }

    }

    @Override
    public void onNumber(double value) throws TException {
        switch (parsePhase) {
            case HEADER:
                fillIntToInvocationCtx((int) value);
                break;
            case BODY:
                DataType.KIND currentType = current.dataType.kind;

                if (!foundField) {
                    return;
                }

                if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                    peek().increaseElement();
                }

                switch (currentType) {
                    case SHORT:
                        oproto.writeI16((short) value);
                        break;
                    case INTEGER:
                    case ENUM:
                        oproto.writeI32((int) value);
                        break;
                    case LONG:
                        oproto.writeI64((long) value);
                        break;
                    case DOUBLE:
                        oproto.writeDouble(value);
                        break;
                    case BIGDECIMAL:
                        oproto.writeString(String.valueOf(value));
                        break;
                    case BYTE:
                        oproto.writeByte((byte) value);
                        break;
                    default:
                        throw new TException("Field:" + current.fieldName + ", DataType(" + current.dataType.kind + ") for " + current.dataType.qualifiedName + " is not a Number");

                }
                break;
            default:
                logger.debug("skip number(" + value + ")@pase:" + parsePhase + " Field:" + current.fieldName);
        }
    }

    @Override
    public void onNumber(long value) throws TException {
        switch (parsePhase) {
            case HEADER:
                fillIntToInvocationCtx((int) value);
                break;
            case BODY:
                DataType.KIND currentType = current.dataType.kind;

                if (!foundField) {
                    return;
                }

                if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                    peek().increaseElement();
                }

                switch (currentType) {
                    case SHORT:
                        oproto.writeI16((short) value);
                        break;
                    case INTEGER:
                    case ENUM:
                        oproto.writeI32((int) value);
                        break;
                    case LONG:
                        oproto.writeI64((long) value);
                        break;
                    case DOUBLE:
                        oproto.writeDouble(value);
                        break;
                    case BYTE:
                        oproto.writeByte((byte) value);
                        break;
                    default:
                        throw new TException("Field:" + current.fieldName + ", DataType(" + current.dataType.kind + ") for " + current.dataType.qualifiedName + " is not a Number");

                }
                break;
            default:
                logger.debug("skip number(" + value + ")@pase:" + parsePhase + " Field:" + current.fieldName);
        }
    }

    @Override
    public void onNull() throws TException {
        switch (parsePhase) {
            case HEADER:
                break;
            case BODY:
                if (!foundField) {
                    return;
                }
                foundNull = true;
                //reset writerIndex, skip the field
                requestByteBuf.writerIndex(current.byteBufPositionBefore);
                if (invocationCtx.codecProtocol() == CompressedBinary) {
                    ((TCompactProtocol)oproto).resetLastFieldId();
                }
                break;
            default:
                logAndThrowTException();
        }
    }

    @Override
    public void onString(String value) throws TException {
        switch (parsePhase) {
            case HEADER:
                fillStringToInvocationCtx(value);
                break;
            case BODY:
                if (!foundField) {
                    return;
                }

                if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                    peek().increaseElement();
                }

                switch (current.dataType.kind) {
                    case ENUM:
                        TEnum tEnum = findEnum(current.dataType.qualifiedName, service);
                        Integer tValue = findEnumItemValue(tEnum, value);
                        if (tValue == null) logAndThrowTException();
                        oproto.writeI32(tValue);
                        break;
                    case BOOLEAN:
                        oproto.writeBool(Boolean.parseBoolean(value));
                        break;
                    case DOUBLE:
                        oproto.writeDouble(Double.parseDouble(value));
                        break;
                    case BIGDECIMAL:
                        oproto.writeString(value);
                        break;
                    case INTEGER:
                        oproto.writeI32(Integer.parseInt(value));
                        break;
                    case LONG:
                        oproto.writeI64(Long.valueOf(value));
                        break;
                    case SHORT:
                        oproto.writeI16(Short.parseShort(value));
                        break;
                    default:
                        if (current.dataType.kind != DataType.KIND.STRING) {
                            throw new TException("Field:" + current.fieldName + ", Not a real String!");
                        }
                        oproto.writeString(value);
                }


                break;
            default:
                logger.debug("skip boolean(" + value + ")@pase:" + parsePhase + " Field:" + current.fieldName);
        }
    }

    private void stackNew(StackNode node) {
        history.push(this.current);
        this.current = node;
    }

    private StackNode pop() {
        return this.current = history.pop();
    }

    private StackNode peek() {
        return history.empty() ? null : history.peek();
    }

    /**
     * 根据current 节点重写集合元素长度
     */
    private void reWriteByteBuf() throws TException {
        assert isMultiElementKind(current.dataType.kind);

        //拿到当前node的开始位置以及集合元素大小
        int beginPosition = current.byteBufPosition;
        int elCount = current.elCount;

        //备份最新的writerIndex
        int currentIndex = requestByteBuf.writerIndex();

        requestByteBuf.writerIndex(beginPosition);

        switch (current.dataType.kind) {
            case MAP:
                reWriteMapBegin(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), elCount);
                break;
            case SET:
            case LIST:
                reWriteCollectionBegin(dataType2Byte(current.dataType.valueType), elCount);
                break;
            default:
                logger.error("Field:" + current.fieldName + ", won't be here", new Throwable());
        }

        if (current.dataType.kind == DataType.KIND.MAP
                && invocationCtx.codecProtocol() == CompressedBinary
                && elCount == 0) {
            requestByteBuf.writerIndex(beginPosition + 1);
        } else {
            requestByteBuf.writerIndex(currentIndex);
        }
    }

    private void writeMapBegin(byte keyType, byte valueType, int defaultSize) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeMapBegin(new TMap(keyType, valueType, defaultSize));
                break;
            case CompressedBinary:
            default:
                JsonUtils.writeMapBegin(keyType, valueType, requestByteBuf);
                break;
        }
    }

    private void reWriteMapBegin(byte keyType, byte valueType, int size) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeMapBegin(new TMap(keyType, valueType, size));
                break;
            case CompressedBinary:
            default:
                JsonUtils.reWriteMapBegin(size, requestByteBuf);
                break;
        }
    }

    /**
     * TList just the same as TSet
     *
     * @param valueType
     * @param defaultSize
     * @throws TException
     */
    private void writeCollectionBegin(byte valueType, int defaultSize) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeListBegin(new TList(valueType, defaultSize));
                break;
            case CompressedBinary:
            default:
                JsonUtils.writeCollectionBegin(valueType, requestByteBuf);
                break;
        }
    }

    private void reWriteCollectionBegin(byte valueType, int size) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeListBegin(new TList(valueType, size));
                break;
            case CompressedBinary:
            default:
                JsonUtils.reWriteCollectionBegin(size, requestByteBuf);
                break;
        }
    }

    private void fillStringToInvocationCtx(String value) {
        if ("calleeIp".equals(currentHeaderName)) {
            invocationCtx.calleeIp(transferIp(value));
        } else if ("callerMid".equals(currentHeaderName)) {
            invocationCtx.callerMid(value);
        } else {
            logger.debug("skip field(" + currentHeaderName + ")@pase:" + parsePhase);
        }
    }


    private void logAndThrowTException() throws TException {
        String fieldName = current == null ? "" : current.fieldName;
        String struct = current == null ? "" : current.struct == null ? (peek().struct == null ? "" : peek().struct.name) : current.struct.name;
        TException ex = new TException("JsonError, please check:"
                + struct + "." + fieldName
                + ", current phase:" + parsePhase);
        logger.error(ex.getMessage(), ex);
        throw ex;
    }

    private void fillIntToInvocationCtx(int value) {
        if ("calleePort".equals(currentHeaderName)) {
            invocationCtx.calleePort(value);
        } else if ("operatorId".equals(currentHeaderName)) {
            invocationCtx.operatorId(Long.valueOf(value));
        } else if ("userId".equals(currentHeaderName)) {
            invocationCtx.userId(Long.valueOf(value));
        } else {
            logger.warn("skip field(" + currentHeaderName + ")@pase:" + parsePhase);
        }
    }
}

