package com.zw.agent.runtime;

public final class AgentRuntimeKeys {

    public static final String AGENT_PREFIX = "agent:";


    private AgentRuntimeKeys() {
    }

    public static String buildAgentKey(Long agentId, Long userId) {
        validateParams(agentId, userId);
        return String.format("%s%s:%s",
                AGENT_PREFIX,agentId, userId);
    }

    public static String redisStateKey(Long userId, Long sessionId) {
        return "agent-state-session_"
                + userId
                + "-"
                + sessionId;
    }

    private static void validateParams(Object... params) {
        for (Object param : params) {
            if (param == null) {
                throw new IllegalArgumentException("缓存Key参数不能为null");
            }
        }
    }
}
