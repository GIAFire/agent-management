import { get, post, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listSkills = () => {
  return get(`${baseUrl}/skillInfo/list`)
}

export const pageSkills = (params) => {
  return get(`${baseUrl}/skillInfo/page`, params)
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
  return get(`${baseUrl}/skillInfo/delete/${stringifyId(id)}`)
}

export const listSkillFilesBySkill = (skillId) => {
  return get(`${baseUrl}/skillFile/skill/${stringifyId(skillId)}`)
}

export const getSkillFileContent = (id) => {
  return get(`${baseUrl}/skillFile/content/${stringifyId(id)}`)
}

export const createSkillPackageNode = (data) => {
  return post(`${baseUrl}/skillFile/createPackageNode`, data)
}

export const updateSkillPackageFile = (data) => {
  return post(`${baseUrl}/skillFile/updatePackageFile`, data)
}

export const deleteSkillPackageNode = (id) => {
  return get(`${baseUrl}/skillFile/deletePackageNode/${stringifyId(id)}`)
}

export const listSkillLogs = () => {
  return get(`${baseUrl}/skillLog/list`)
}
