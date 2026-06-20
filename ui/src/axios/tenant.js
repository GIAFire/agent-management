import { del, get, post, put } from '@/axios/request'

const baseUrl = '/tenant'

export const listTenant = () => {
  return get(`${baseUrl}/list`)
}

export const pageTenant = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getTenant = (id) => {
  return get(`${baseUrl}/${id}`)
}

export const addTenant = (data) => {
  return post(baseUrl, data)
}

export const updateTenant = (data) => {
  return put(baseUrl, data)
}

export const deleteTenant = (id) => {
  return del(`${baseUrl}/${id}`)
}
