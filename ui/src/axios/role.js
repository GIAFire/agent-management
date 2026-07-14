import { get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent/sysRole'

export const listRole = () => {
  return get(`${baseUrl}/list`)
}

export const pageRole = (params) => {
  return get(`${baseUrl}/page`, params)
}

export const getRole = (id) => {
  return get(`${baseUrl}/${stringifyId(id)}`)
}

export const addRole = (data) => {
  return post(`${baseUrl}/create`, data)
}

export const updateRole = (data) => {
  return post(`${baseUrl}/update`, data)
}

export const deleteRole = (id) => {
  return get(`${baseUrl}/delete/${stringifyId(id)}`)
}
