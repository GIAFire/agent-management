import { get } from '@/axios/request'

const baseUrl = '/agent'

export const listWizardModels = () => {
  return get(`${baseUrl}/modelConfig/list`)
}

export const listWizardPrompts = () => {
  return get(`${baseUrl}/sysPrompt/list`)
}

export const listWizardTools = () => {
  return get(`${baseUrl}/toolInfoConfig/list`)
}

export const listWizardKnowledgeBases = () => {
  return get(`${baseUrl}/knowledgeBase/list`)
}

export const listWizardSubagents = () => {
  return get(`${baseUrl}/subagent/list`)
}
