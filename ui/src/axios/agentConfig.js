import { del, get, post, put } from '@/axios/request'

const baseUrl = '/agentConfig'

export const listAgentConfig = () => {
  return get(`${baseUrl}/list`)
}

export const pageAgentConfig = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getAgentConfig = (id) => {
  return get(`${baseUrl}/${id}`)
}

export const addAgentConfig = (data) => {
  return post(baseUrl, data)
}

export const updateAgentConfig = (data) => {
  return put(baseUrl, data)
}

export const deleteAgentConfig = (id) => {
  return del(`${baseUrl}/${id}`)
}
