import { get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listSubagents = () => {
  return get(`${baseUrl}/subagent/list`)
}

export const pageSubagents = (params) => {
  return get(`${baseUrl}/subagent/page`, params)
}

export const getSubagent = (id) => {
  return get(`${baseUrl}/subagent/${stringifyId(id)}`)
}

export const createSubagent = (data) => {
  return post(`${baseUrl}/subagent/create`, data)
}

export const updateSubagent = (data) => {
  return post(`${baseUrl}/subagent/update`, data)
}

export const deleteSubagent = (id) => {
  return get(`${baseUrl}/subagent/delete/${stringifyId(id)}`)
}

export const listSubagentTasks = () => {
  return get(`${baseUrl}/subagentTask/list`)
}

