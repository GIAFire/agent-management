import { del, get, post, put, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listModelConfig = () => {
  return get(`${baseUrl}/modelConfig/list`)
}

export const pageModelConfig = (params) => {
  return get(`${baseUrl}/modelConfig/page`, params)
}

export const getModelConfig = (id) => {
  return get(`${baseUrl}/modelConfig/${stringifyId(id)}`)
}

export const addModelConfig = (data) => {
  return post(`${baseUrl}/modelConfig/save`, data)
}

export const updateModelConfig = (data) => {
  return put(`${baseUrl}/modelConfig`, data)
}

export const deleteModelConfig = (id) => {
  return del(`${baseUrl}/modelConfig/${stringifyId(id)}`)
}
