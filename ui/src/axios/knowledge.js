import { del, get, post, request, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const listKnowledgeBases = (params) => {
  return get(`${baseUrl}/knowledgeBases`, params)
}

export const getKnowledgeMetrics = () => get(`${baseUrl}/knowledgeBases/metrics`)

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
  return request({
    url: `${baseUrl}/knowledgeBases/${stringifyId(knowledgeBaseId)}`,
    method: 'delete',
    timeout: 120000
  })
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

export const submitDocumentIndex = (documentId, data) => {
  return post(`${baseUrl}/knowledgeDocuments/${stringifyId(documentId)}/index`, data)
}
