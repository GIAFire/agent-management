import { del, get, post, put } from '@/axios/request'

const baseUrl = '/modelConfig'

export const listModelConfig = () => {
  return get(`${baseUrl}/list`)
}

export const pageModelConfig = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getModelConfig = (id) => {
  return get(`${baseUrl}/${id}`)
}

export const addModelConfig = (data) => {
  return post(baseUrl, data)
}

export const updateModelConfig = (data) => {
  return put(baseUrl, data)
}

export const deleteModelConfig = (id) => {
  return del(`${baseUrl}/${id}`)
}
