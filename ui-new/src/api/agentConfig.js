import { del, get, post, put, stringifyId } from '@/axios/request'

export const listAgentConfigs = () => get('/agent/agentConfig/list')

export const pageAgentConfigs = (params) => get('/agent/agentConfig/page', params)

export const getAgentConfig = (id) => get(`/agent/agentConfig/${stringifyId(id)}`)

export const createAgentConfig = (data) => post('/agent/agentConfig', data)

export const updateAgentConfig = (data) => put('/agent/agentConfig', data)

export const deleteAgentConfig = (id) => del(`/agent/agentConfig/${stringifyId(id)}`)

