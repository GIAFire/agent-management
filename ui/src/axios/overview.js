import { get } from '@/axios/request'

const baseUrl = '/agent/overview'

export const getRunOverview = (params) => get(baseUrl, params)

export const getInteractionHistory = (params) => get(`${baseUrl}/interactions/page`, params)
