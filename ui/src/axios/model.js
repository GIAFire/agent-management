import { del, get, post, put } from '@/axios/request'

const baseUrl = '/agent'

export const listModelConfig = () => {
  return get(`${baseUrl}/modelConfig/list`)
}

export const pageModelConfig = (params) => {
  return get(`${baseUrl}/modelConfig/page`, params)
}

export const getModelConfig = (id) => {
  return get(`${baseUrl}/modelConfig/${id}`)
}

export const addModelConfig = (data) => {
  return post(`${baseUrl}/modelConfig/save`, data)
}

export const updateModelConfig = (data) => {
  return put(baseUrl, data)
}

export const deleteModelConfig = (id) => {
  return del(`${baseUrl}/modelConfig/${id}`)
}
