import { del, get, post, put, stringifyId } from '@/axios/request'

const baseUrl = '/agent/agent'

export const getAgentMetrics = () => get(`${baseUrl}/metrics`)

export const pageAgents = (params) => get(`${baseUrl}/page`, params)

export const getAgent = (id) => get(`${baseUrl}/${stringifyId(id)}`)

export const createAgent = (data) => post(baseUrl, data)

export const updateAgent = (id, data) => put(
  `${baseUrl}/${stringifyId(id)}`,
  data
)

export const deleteAgent = (id) => del(`${baseUrl}/${stringifyId(id)}`)

export const pageAgentRuns = (id, params) => get(
  `${baseUrl}/${stringifyId(id)}/runs`,
  params
)
