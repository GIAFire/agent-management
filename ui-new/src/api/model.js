import { del, get, post, put, stringifyId } from '@/axios/request'

export const listModelConfigs = () => get('/agent/modelConfig/list')

export const pageModelConfigs = (params) => get('/agent/modelConfig/page', params)

export const getModelConfig = (id) => get(`/agent/modelConfig/${stringifyId(id)}`)

export const createModelConfig = (data) => post('/agent/modelConfig/save', data)

export const updateModelConfig = (data) => put('/agent/modelConfig', data)

export const deleteModelConfig = (id) => del(`/agent/modelConfig/${stringifyId(id)}`)

