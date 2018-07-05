package com.github.dapeng.json;

import com.github.dapeng.org.apache.thrift.TException;

/**
 * @author ever
 */
public interface JsonCallback {

    /**
     *
     * @throws TException
     */
    void onStartObject() throws TException;
    void onEndObject() throws TException;

    void onStartArray() throws TException;
    void onEndArray() throws TException;

    void onStartField(String name) throws TException;
    void onEndField() throws TException;

    void onBoolean(boolean value) throws TException;
    void onNumber(double value) throws TException;
    void onNumber(long value) throws TException;
    void onNull() throws TException;

    void onString(String value) throws TException;
}
