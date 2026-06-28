import { del, get, post, put, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listAgentConfig = () => {
  return get(`${baseUrl}/agentConfig/list`)
}

export const pageAgentConfig = (params) => {
  return get(`${baseUrl}/agentConfig/page`, params)
}

export const getAgentConfig = (id) => {
  return get(`${baseUrl}/agentConfig/${stringifyId(id)}`)
}

export const addAgentConfig = (data) => {
  return post(`${baseUrl}/agentConfig`, data)
}

export const updateAgentConfig = (data) => {
  return put(`${baseUrl}/agentConfig`, data)
}

export const deleteAgentConfig = (id) => {
  return del(`${baseUrl}/agentConfig/${stringifyId(id)}`)
}
