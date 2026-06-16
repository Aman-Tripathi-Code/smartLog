package com.smartlog.sdk.sender;

public interface LogTransport {

    boolean post(String jsonPayload) throws Exception;
}
