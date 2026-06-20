import { del, get, post, put } from '@/axios/request'

const baseUrl = '/agent'

export const listAgent = () => {
  return get(`${baseUrl}/list`)
}

export const pageAgent = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getAgent = (id) => {
  return get(`${baseUrl}/${id}`)
}

export const addAgent = (data) => {
  return post(baseUrl, data)
}

export const updateAgent = (data) => {
  return put(baseUrl, data)
}

export const deleteAgent = (id) => {
  return del(`${baseUrl}/${id}`)
}
