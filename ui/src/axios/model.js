import { get, post, stringifyId } from '@/axios/request'

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
  return post(`${baseUrl}/modelConfig/create`, data)
}

export const updateModelConfig = (data) => {
  return post(`${baseUrl}/modelConfig/update`, data)
}

export const deleteModelConfig = (id) => {
  return get(`${baseUrl}/modelConfig/delete/${stringifyId(id)}`)
}
