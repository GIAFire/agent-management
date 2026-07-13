import { get, post } from '@/axios/request'

const baseUrl = '/agent'

export const listTools = () => {
  return get(`${baseUrl}/toolInfoConfig/list`)
}

export const createTool = (data) => {
  return post(`${baseUrl}/toolInfoConfig/create`, data)
}

export const updateTool = (data) => {
  return post(`${baseUrl}/toolInfoConfig/update`, data)
}

export const listToolGroups = () => {
  return get(`${baseUrl}/toolGroupConfig/list`)
}

export const listToolCallLogs = () => {
  return get(`${baseUrl}/toolCallLog/list`)
}

export const listToolPermissions = () => {
  return get(`${baseUrl}/toolRolePermission/list`)
}
