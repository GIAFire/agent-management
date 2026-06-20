package com.zw.agent.exception;

public class AgentConfigException extends RuntimeException {

    public AgentConfigException(String message) {
        super(message);
    }

    public AgentConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}