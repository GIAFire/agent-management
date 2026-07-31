import { del, get, post, request, stringifyId } from '@/axios/request'

const baseUrl = '/agent/modelConfig'

export const getModelMetrics = () => get(`${baseUrl}/metrics`)

export const getModelAnalytics = (days = 7) => get(`${baseUrl}/analytics`, { days })

export const listModelConfig = () => get(`${baseUrl}/list`)

export const pageModelConfig = (params) => get(`${baseUrl}/page`, params)

export const getModelConfig = (id) => get(`${baseUrl}/${stringifyId(id)}`)

export const addModelConfig = (data) => post(`${baseUrl}/create`, data)

export const updateModelConfig = (data) => post(`${baseUrl}/update`, data)

export const deleteModelConfig = (id) => del(`${baseUrl}/${stringifyId(id)}`)

export const testModelConfig = (data) => request({
  url: `${baseUrl}/test`,
  method: 'post',
  data,
  timeout: 620000
})

export const pageModelCallLogs = (params) => get(`${baseUrl}/logs/page`, params)
