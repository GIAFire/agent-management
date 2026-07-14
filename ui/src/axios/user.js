import { get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent/sysUser'

export const listUser = () => {
  return get(`${baseUrl}/list`)
}

export const pageUser = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getUser = (id) => {
  return get(`${baseUrl}/${stringifyId(id)}`)
}

export const addUser = (data) => {
  return post(`${baseUrl}/create`, data)
}

export const updateUser = (data) => {
  return post(`${baseUrl}/update`, data)
}

export const deleteUser = (id) => {
  return get(`${baseUrl}/delete/${stringifyId(id)}`)
}
