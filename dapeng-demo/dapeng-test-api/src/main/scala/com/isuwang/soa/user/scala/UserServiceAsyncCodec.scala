package com.isuwang.soa.user.scala
        import com.isuwang.soa.user.scala.domain.serializer._;import com.isuwang.soa.price.scala.domain.serializer._;import com.isuwang.soa.order.scala.domain.serializer._;import com.github.dapeng.soa.scala.domain.serializer._;import com.isuwang.soa.settle.scala.domain.serializer._;

        import com.github.dapeng.core._
        import com.github.dapeng.org.apache.thrift._
        import com.github.dapeng.org.apache.thrift.protocol._
        import com.github.dapeng.core.definition._

        import scala.concurrent.ExecutionContext.Implicits.global
        import java.util.concurrent.{CompletableFuture, Future}
        import scala.util.{Failure, Success}

        /**
        * Autogenerated by Dapeng-Code-Generator (2.0.0)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *  @generated
        **/
        object UserServiceAsyncCodec {

        implicit class FutureX[T](f: scala.concurrent.Future[T]) {
          def tojava(): CompletableFuture[T] = {
            val java = new CompletableFuture[T]()
            f.onComplete{
              case Success(x) => java.complete(x)
              case Failure(ex) => java.completeExceptionally(ex)
            }
            java
          }
        }

        
            case class createUser_args(user:com.isuwang.soa.user.scala.domain.User)

            case class createUser_result()

            class CreateUser_argsSerializer extends BeanSerializer[createUser_args]{
            
      @throws[TException]
      override def read(iprot: TProtocol): createUser_args = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      var user: com.isuwang.soa.user.scala.domain.User = null
        

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
              case 1 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT => user = 
          new com.isuwang.soa.user.scala.domain.serializer.UserSerializer().read(iprot)
        
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = createUser_args(user = user)
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: createUser_args, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("createUser_args"))

      
            {
            val elem0 = bean.user 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("user", com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, 1.asInstanceOf[Short]))
            
          new com.isuwang.soa.user.scala.domain.serializer.UserSerializer().write(elem0, oprot)
        
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: createUser_args): Unit = {
      
              if(bean.user == null)
              throw new SoaException(SoaCode.NotNull, "user字段不允许为空")
            
                if(bean.user != null)
                new com.isuwang.soa.user.scala.domain.serializer.UserSerializer().validate(bean.user)
              
    }
    

            override def toString(bean: createUser_args): String = if(bean == null)  "null" else bean.toString
          }

            class CreateUser_resultSerializer extends BeanSerializer[createUser_result]{

            @throws[TException]
            override def read(iprot: TProtocol): createUser_result = {

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

                schemeField = iprot.readFieldBegin

                schemeField.id match {
                  case 0 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.VOID =>  com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  }
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }

                iprot.readFieldEnd
              }

              iprot.readStructEnd
              val bean = createUser_result()
              validate(bean)

              bean
            }

            
      @throws[TException]
      override def write(bean: createUser_result, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("createUser_result"))

      
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
            
      @throws[TException]
      override def validate(bean: createUser_result): Unit = {
      
    }
    

            override def toString(bean: createUser_result): String = if(bean == null)  "null" else bean.toString
          }

            class createUser extends SoaFunctionDefinition.Async[com.isuwang.soa.user.scala.service.UserServiceAsync, createUser_args, createUser_result]("createUser", new CreateUser_argsSerializer(), new CreateUser_resultSerializer()){

            @throws[TException]
            def apply(iface: com.isuwang.soa.user.scala.service.UserServiceAsync, args: createUser_args):Future[createUser_result] = {

              val _result = iface.createUser(args.user)

              
                _result.map(i => createUser_result()).tojava

              

            }
          }
          
            case class getUserById_args(userId:Int)

            case class getUserById_result(success:com.isuwang.soa.user.scala.domain.User)

            class GetUserById_argsSerializer extends BeanSerializer[getUserById_args]{
            
      @throws[TException]
      override def read(iprot: TProtocol): getUserById_args = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      var userId: Int = 0
        

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
              case 1 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.I32 => userId = iprot.readI32
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = getUserById_args(userId = userId)
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: getUserById_args, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getUserById_args"))

      
            {
            val elem0 = bean.userId 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("userId", com.github.dapeng.org.apache.thrift.protocol.TType.I32, 1.asInstanceOf[Short]))
            oprot.writeI32(elem0)
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: getUserById_args): Unit = {
      
    }
    

            override def toString(bean: getUserById_args): String = if(bean == null)  "null" else bean.toString
          }

            class GetUserById_resultSerializer extends BeanSerializer[getUserById_result]{

            @throws[TException]
            override def read(iprot: TProtocol): getUserById_result = {

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              var success : com.isuwang.soa.user.scala.domain.User = null

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

                schemeField = iprot.readFieldBegin

                schemeField.id match {
                  case 0 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT =>  success = 
          new com.isuwang.soa.user.scala.domain.serializer.UserSerializer().read(iprot)
        
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  }
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }

                iprot.readFieldEnd
              }

              iprot.readStructEnd
              val bean = getUserById_result(success)
              validate(bean)

              bean
            }

            
      @throws[TException]
      override def write(bean: getUserById_result, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getUserById_result"))

      
            {
            val elem0 = bean.success 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, 0.asInstanceOf[Short]))
            
          new com.isuwang.soa.user.scala.domain.serializer.UserSerializer().write(elem0, oprot)
        
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
            
      @throws[TException]
      override def validate(bean: getUserById_result): Unit = {
      
              if(bean.success == null)
              throw new SoaException(SoaCode.NotNull, "success字段不允许为空")
            
                if(bean.success != null)
                new com.isuwang.soa.user.scala.domain.serializer.UserSerializer().validate(bean.success)
              
    }
    

            override def toString(bean: getUserById_result): String = if(bean == null)  "null" else bean.toString
          }

            class getUserById extends SoaFunctionDefinition.Async[com.isuwang.soa.user.scala.service.UserServiceAsync, getUserById_args, getUserById_result]("getUserById", new GetUserById_argsSerializer(), new GetUserById_resultSerializer()){

            @throws[TException]
            def apply(iface: com.isuwang.soa.user.scala.service.UserServiceAsync, args: getUserById_args):Future[getUserById_result] = {

              val _result = iface.getUserById(args.userId)

              _result.map(getUserById_result(_)).tojava

            }
          }
          

        case class getServiceMetadata_args()

        case class getServiceMetadata_result(success: String)

        class GetServiceMetadata_argsSerializer extends BeanSerializer[getServiceMetadata_args] {

          @throws[TException]
          override def read(iprot: TProtocol): getServiceMetadata_args = {

            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
              schemeField = iprot.readFieldBegin
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              iprot.readFieldEnd
            }

            iprot.readStructEnd

            val bean = getServiceMetadata_args()
            validate(bean)

            bean
          }

          @throws[TException]
          override def write(bean: getServiceMetadata_args, oproto: TProtocol): Unit = {
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_args"))

            oproto.writeFieldStop
            oproto.writeStructEnd
          }

          @throws[TException]
          override def validate(bean: getServiceMetadata_args): Unit = {}

          override def toString(bean: getServiceMetadata_args): String = if (bean == null) "null" else bean.toString
        }



        class GetServiceMetadata_resultSerializer extends BeanSerializer[getServiceMetadata_result] {
          @throws[TException]
          override def read(iprot: TProtocol): getServiceMetadata_result = {
            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            var success: String = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
              schemeField = iprot.readFieldBegin

              schemeField.id match {
                case 0 =>
                schemeField.`type` match {
                  case com.github.dapeng.org.apache.thrift.protocol.TType.STRING => success = iprot.readString
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }
                case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              }
              iprot.readFieldEnd
            }

            iprot.readStructEnd
            val bean = getServiceMetadata_result(success)
            validate(bean)

            bean
          }

          @throws[TException]
          override def write(bean: getServiceMetadata_result, oproto: TProtocol): Unit = {
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_result"))

            oproto.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oproto.writeString(bean.success)
            oproto.writeFieldEnd

            oproto.writeFieldStop
            oproto.writeStructEnd
          }

          @throws[TException]
          override def validate(bean: getServiceMetadata_result): Unit = {
            if (bean.success == null)
            throw new SoaException(SoaCode.NotNull, "success字段不允许为空")
          }

          override def toString(bean: getServiceMetadata_result): String = if (bean == null) "null" else bean.toString

        }



        class getServiceMetadata extends SoaFunctionDefinition.Async[com.isuwang.soa.user.scala.service.UserServiceAsync, getServiceMetadata_args, getServiceMetadata_result](
        "getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer()) {


          @throws[TException]
          override def apply(iface: com.isuwang.soa.user.scala.service.UserServiceAsync, args: getServiceMetadata_args): Future[getServiceMetadata_result] = {

            val result = scala.concurrent.Future {
            val source = scala.io.Source.fromInputStream(UserServiceCodec.getClass.getClassLoader.getResourceAsStream("com.isuwang.soa.user.service.UserService.xml"))
            val success = source.mkString
            source.close
            getServiceMetadata_result(success)
            }
            result.tojava

          }
        }

        class Processor(iface: com.isuwang.soa.user.scala.service.UserServiceAsync, ifaceClass: Class[com.isuwang.soa.user.scala.service.UserServiceAsync]) extends
        SoaServiceDefinition(iface,classOf[com.isuwang.soa.user.scala.service.UserServiceAsync], Processor.buildMap)

        object Processor{

          type PF = SoaFunctionDefinition[com.isuwang.soa.user.scala.service.UserServiceAsync, _, _]

          def buildMap(): java.util.Map[String, PF] = {
            val map = new java.util.HashMap[String, PF]()
            map.put("createUser", new createUser)
              map.put("getUserById", new getUserById)
              
            map.put("getServiceMetadata", new getServiceMetadata)
            map
          }

        }
      }
      