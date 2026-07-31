import { del, get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listSkills = () => {
  return get(`${baseUrl}/skillInfo/list`)
}

export const pageSkills = (params) => {
  return get(`${baseUrl}/skillInfo/page`, params)
}

export const getSkillMetrics = () => {
  return get(`${baseUrl}/skillInfo/metrics`)
}

export const getSkill = (id) => {
  return get(`${baseUrl}/skillInfo/${stringifyId(id)}`)
}

export const createSkill = (data) => {
  return post(`${baseUrl}/skillInfo/create`, data)
}

export const updateSkill = (data) => {
  return post(`${baseUrl}/skillInfo/update`, data)
}

export const deleteSkill = (id) => {
  return del(`${baseUrl}/skillInfo/${stringifyId(id)}`)
}

export const listSkillFilesBySkill = (skillId) => {
  return get(`${baseUrl}/skillResource/skill/${stringifyId(skillId)}`)
}

export const getSkillFileContent = (id) => {
  return get(`${baseUrl}/skillResource/content/${stringifyId(id)}`)
}

export const createSkillPackageNode = (data) => {
  return post(`${baseUrl}/skillResource/create`, data)
}

export const updateSkillPackageFile = (data) => {
  return post(`${baseUrl}/skillResource/update`, data)
}

export const deleteSkillPackageNode = (id) => {
  return del(`${baseUrl}/skillResource/${stringifyId(id)}`)
}

export const deleteSkillPackageFolder = (skillId, path) => {
  return del(`${baseUrl}/skillResource/folder`, {
    skillId: stringifyId(skillId),
    path
  })
}

export const pageSkillLogs = (params) => {
  return get(`${baseUrl}/skillInfo/logs/page`, params)
}

export const listRecentSkillLogs = (params) => {
  return get(`${baseUrl}/skillInfo/logs/recent`, params)
}
