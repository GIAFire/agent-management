import { get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent/sysUserRole'

export const listUserRole = () => {
  return get(`${baseUrl}/list`)
}

export const pageUserRole = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getUserRole = (id) => {
  return get(`${baseUrl}/${stringifyId(id)}`)
}

export const addUserRole = (data) => {
  return post(`${baseUrl}/create`, data)
}

export const updateUserRole = (data) => {
  return post(`${baseUrl}/update`, data)
}

export const deleteUserRole = (id) => {
  return get(`${baseUrl}/delete/${stringifyId(id)}`)
}
