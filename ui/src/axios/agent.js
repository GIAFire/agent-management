import { del, get, post, put } from '@/axios/request'

const baseUrl = '/agent'

export const listAgent = () => {
  return get(`${baseUrl}/agent/list`)
}

export const pageAgent = (params) => {
  return get(`${baseUrl}/agent/page`, params)
}

export const getAgent = (id) => {
  return get(`${baseUrl}/agent/${id}`)
}

export const addAgent = (data) => {
  return post(baseUrl, data)
}

export const updateAgent = (data) => {
  return put(baseUrl, data)
}

export const deleteAgent = (id) => {
  return del(`${baseUrl}/agent/${id}`)
}
