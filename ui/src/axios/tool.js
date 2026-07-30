import { get, post } from '@/axios/request'

const baseUrl = '/agent'

export const listTools = () => {
  return get(`${baseUrl}/toolInfoConfig/list`)
}

export const getToolMetrics = () => {
  return get(`${baseUrl}/toolInfoConfig/metrics`)
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

export const pageToolCallLogs = (params) => {
  return get(`${baseUrl}/toolCallLog/management/page`, params)
}

export const pageToolPermissions = (params) => {
  return get(`${baseUrl}/toolRolePermission/management/page`, params)
}

export const saveToolPermission = (data) => {
  return post(`${baseUrl}/toolRolePermission/management/save`, data)
}

export const disableToolPermission = (id) => {
  return post(`${baseUrl}/toolRolePermission/management/${id}/disable`)
}
