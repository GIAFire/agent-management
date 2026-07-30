import { del, get, post, request, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listKnowledgeBases = (params) => {
  return get(`${baseUrl}/knowledgeBases`, params)
}

export const getKnowledgeBase = (knowledgeBaseId) => {
  return get(`${baseUrl}/knowledgeBases/${stringifyId(knowledgeBaseId)}`)
}

export const listKnowledgeBaseOptions = () => {
  return get(`${baseUrl}/knowledgeBases/options`)
}

export const createKnowledgeBase = (data) => {
  return request({
    url: `${baseUrl}/knowledgeBases`,
    method: 'post',
    data,
    timeout: 120000
  })
}

export const updateKnowledgeBase = (knowledgeBaseId, data) => {
  return request({
    url: `${baseUrl}/knowledgeBases/${stringifyId(knowledgeBaseId)}`,
    method: 'put',
    data,
    timeout: 120000
  })
}

export const deleteKnowledgeBase = (knowledgeBaseId) => {
  return del(`${baseUrl}/knowledgeBases/${stringifyId(knowledgeBaseId)}`)
}

export const listKnowledgeDocuments = (knowledgeBaseId, params) => {
  return get(`${baseUrl}/knowledgeBases/${stringifyId(knowledgeBaseId)}/documents`, params)
}

export const uploadKnowledgeDocument = (knowledgeBaseId, file) => {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: `${baseUrl}/knowledgeBases/${stringifyId(knowledgeBaseId)}/documents`,
    method: 'post',
    data: formData,
    timeout: 120000
  })
}

export const deleteKnowledgeDocument = (documentId) => {
  return del(`${baseUrl}/knowledgeDocuments/${stringifyId(documentId)}`)
}

export const listKnowledgeChunks = (documentId, params) => {
  return get(`${baseUrl}/knowledgeDocuments/${stringifyId(documentId)}/chunks`, params)
}

export const createDocumentIndexTask = (documentId, data) => {
  return post(`${baseUrl}/knowledgeDocuments/${stringifyId(documentId)}/indexTasks`, data)
}

export const listDocumentTasks = (documentId, params) => {
  return get(`${baseUrl}/knowledgeDocuments/${stringifyId(documentId)}/tasks`, params)
}

export const getKnowledgeTask = (taskId, options = {}) => {
  return request({
    url: `${baseUrl}/knowledgeTasks/${stringifyId(taskId)}`,
    method: 'get',
    skipErrorMessage: options.skipErrorMessage === true
  })
}

export const resubmitKnowledgeTask = (taskId) => {
  return post(`${baseUrl}/knowledgeTasks/${stringifyId(taskId)}/resubmit`)
}
