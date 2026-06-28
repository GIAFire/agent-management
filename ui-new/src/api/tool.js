import { del, get, post, put, stringifyId } from '@/axios/request'

export const listTools = (options = {}) => get('/agent/toolConfig/list', {}, options)

export const pageTools = (params) => get('/agent/toolConfig/page', params)

export const getTool = (id) => get(`/agent/toolConfig/${stringifyId(id)}`)

export const createTool = (data) => post('/agent/toolConfig/create', data)

export const updateTool = (data) => put('/agent/toolConfig/update', data)

export const deleteTool = (id) => del(`/agent/toolConfig/delete/${stringifyId(id)}`)

export const listToolGroups = (options = {}) => get('/agent/toolGroupConfig/list', {}, options)

export const createToolGroup = (data) => post('/agent/toolGroupConfig/create', data)

export const updateToolGroup = (data) => put('/agent/toolGroupConfig/update', data)

export const deleteToolGroup = (id) => del(`/agent/toolGroupConfig/delete/${stringifyId(id)}`)
