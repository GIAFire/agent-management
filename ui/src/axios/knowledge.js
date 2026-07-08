import { get, request, stringifyId } from '@/axios/request'

const baseUrl = '/agent'

export const uploadKnowledgeDocument = ({ file, knowledgeBaseId, workspaceFileId, language }) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('knowledgeBaseId', stringifyId(knowledgeBaseId))
  if (workspaceFileId !== '' && workspaceFileId !== undefined && workspaceFileId !== null) {
    formData.append('workspaceFileId', stringifyId(workspaceFileId))
  }
  if (language) {
    formData.append('language', language)
  }

  return request({
    url: `${baseUrl}/ai-knowledge-document-entity/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}

export const indexKnowledgeDocument = (data) => {
  return request({
    url: `${baseUrl}/chunk/index`,
    method: 'post',
    data,
    timeout: 180000
  })
}

export const listKnowledgeChunks = (documentId) => {
  return get(`${baseUrl}/chunk/document/${stringifyId(documentId)}`)
}
