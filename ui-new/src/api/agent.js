import { del, get, post, put, stringifyId } from '@/axios/request'

export const listAgents = () => get('/agent/agent/list')

export const pageAgents = (params) => get('/agent/agent/page', params)

export const getAgent = (id) => get(`/agent/agent/${stringifyId(id)}`)

export const createAgent = (data) => post('/agent/agent/create', data)

export const updateAgent = (data) => put('/agent/agent/update', data)

export const deleteAgent = (id) => del(`/agent/agent/${stringifyId(id)}`)

