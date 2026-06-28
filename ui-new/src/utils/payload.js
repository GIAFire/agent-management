import { getUser } from '@/utils/auth'

export const currentTenantId = () => {
  return getUser()?.tenantId || '1'
}

export const currentUserId = () => {
  return getUser()?.id || getUser()?.userId || '1'
}

export const withAuditFields = (data = {}, mode = 'create') => {
  const now = new Date().toISOString().slice(0, 19).replace('T', ' ')
  const userId = currentUserId()
  const tenantId = currentTenantId()

  return {
    ...data,
    tenantId: data.tenantId || tenantId,
    deleted: data.deleted ?? 0,
    ...(mode === 'create'
      ? {
          createdAt: data.createdAt || now,
          createdBy: data.createdBy || userId
        }
      : {}),
    updatedAt: now,
    updateBy: userId
  }
}

export const safeParseJson = (value, fallback = {}) => {
  if (!value) {
    return fallback
  }
  if (typeof value === 'object') {
    return value
  }
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

export const prettyJson = (value) => {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return '{}'
  }
}

