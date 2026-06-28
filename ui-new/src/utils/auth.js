const TOKEN_KEY = 'agent_scope_token'
const TOKEN_TYPE_KEY = 'agent_scope_token_type'
const TOKEN_EXPIRES_AT_KEY = 'agent_scope_token_expires_at'
const USER_KEY = 'agent_scope_user'

export const getToken = () => sessionStorage.getItem(TOKEN_KEY) || ''

export const getTokenType = () => sessionStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'

export const getUser = () => {
  const raw = sessionStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export const saveLoginSession = (loginData) => {
  if (!loginData?.token) {
    return
  }
  sessionStorage.setItem(TOKEN_KEY, loginData.token)
  sessionStorage.setItem(TOKEN_TYPE_KEY, loginData.tokenType || 'Bearer')
  sessionStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(loginData.expiresAt || ''))
  if (loginData.user) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(loginData.user))
  }
}

const decodeJwtPayload = (token) => {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(normalized)
        .split('')
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return {}
  }
}

export const saveAccessToken = (token, tokenType = 'Bearer') => {
  if (!token) {
    return
  }

  const payload = decodeJwtPayload(token)
  sessionStorage.setItem(TOKEN_KEY, token)
  sessionStorage.setItem(TOKEN_TYPE_KEY, tokenType || 'Bearer')
  if (payload.exp) {
    sessionStorage.setItem(TOKEN_EXPIRES_AT_KEY, String(payload.exp * 1000))
  }
  sessionStorage.setItem(
    USER_KEY,
    JSON.stringify({
      id: payload.userId || payload.sub || '',
      userName: payload.userName || payload.sub || 'admin',
      tenantId: payload.tenantId || ''
    })
  )
}

export const clearAuth = () => {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_TYPE_KEY)
  sessionStorage.removeItem(TOKEN_EXPIRES_AT_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export const isLoggedIn = () => Boolean(getToken())
