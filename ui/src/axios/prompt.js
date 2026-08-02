import { del, get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent/sysPrompt'

export const getPromptMetrics = () => get(`${baseUrl}/metrics`)

export const getPromptAnalytics = (limit = 5) => get(`${baseUrl}/analytics`, { limit })

export const pagePrompts = (params) => get(`${baseUrl}/page`, params)

export const getPrompt = (id) => get(`${baseUrl}/${stringifyId(id)}`)

export const createPrompt = (data) => post(`${baseUrl}/create`, data)

export const updatePrompt = (data) => post(`${baseUrl}/update`, data)

export const deletePrompt = (id) => del(`${baseUrl}/${stringifyId(id)}`)
