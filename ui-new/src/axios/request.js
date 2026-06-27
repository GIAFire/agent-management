import axios from 'axios'
import { ElMessage } from 'element-plus'
import config from '@/axios/axiosConfig'
import { clearAuth, getToken, getTokenType, saveLoginSession } from '@/utils/auth'

export const BASE_URL = config.baseURL

const USER_KEY = 'agent_scope_user'

const service = axios.create({
  baseURL: config.baseURL,
  timeout: config.timeout
})

const isPlainObject = (value) => {
  return Object.prototype.toString.call(value) === '[object Object]'
}

const isIdKey = (key) => {
  return key === 'id' || key === 'ids' || key.endsWith('Id') || key.endsWith('Ids') || key.endsWith('ID') || key.endsWith('IDs')
}

export const stringifyId = (value) => {
  if (value === '' || value === undefined || value === null) {
    return ''
  }

  return String(value)
}

const stringifyIdFieldValue = (value) => {
  if (Array.isArray(value)) {
    return value.map((item) => stringifyIdFieldValue(item))
  }

  if (value === '' || value === undefined || value === null) {
    return value === '' ? '' : null
  }

  return String(value)
}

const stringifyIdFields = (value, key = '') => {
  if (isIdKey(key)) {
    return stringifyIdFieldValue(value)
  }

  if (Array.isArray(value)) {
    return value.map((item) => stringifyIdFields(item))
  }

  if (!isPlainObject(value)) {
    return value
  }

  return Object.entries(value).reduce((result, [entryKey, entryValue]) => {
    result[entryKey] = stringifyIdFields(entryValue, entryKey)
    return result
  }, {})
}

service.interceptors.request.use(
  (requestConfig) => {
    const token = getToken()
    if (token) {
      requestConfig.headers = requestConfig.headers || {}
      requestConfig.headers.Authorization = `${getTokenType()} ${token}`
      const user = JSON.parse(sessionStorage.getItem(USER_KEY))
    }
    if (requestConfig.params) {
      requestConfig.params = stringifyIdFields(requestConfig.params)
    }
    if (requestConfig.data) {
      requestConfig.data = stringifyIdFields(requestConfig.data)
    }
    return requestConfig
  },
  (error) => Promise.reject(error)
)

const redirectToLogin = () => {
  const currentPath = `${window.location.pathname}${window.location.search}`
  if (window.location.pathname !== '/login') {
    window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
  }
}

service.interceptors.response.use(
  (response) => {
    const body = response.data

    if (body && Object.prototype.hasOwnProperty.call(body, 'code')) {
      if (body.code !== 200) {
        if (body.code === 401) {
          clearAuth()
          redirectToLogin()
        }
        ElMessage.error(body.msg || '接口请求失败')
        return Promise.reject(new Error(body.msg || '接口请求失败'))
      }

      if (response.config?.url?.includes('/auth/auth/login')) {
        saveLoginSession(body.data)
      }
      return body.data
    }

    return body
  },
  (error) => {
    const errorData = error.response?.data
    const message = errorData?.msg || errorData?.error || error.message || '网络请求异常'
    if (error.response?.status === 401) {
      clearAuth()
      redirectToLogin()
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export const request = (options) => service(options)

export const get = (url, params = {}) => {
  return service({
    url,
    method: 'get',
    params
  })
}

export const post = (url, data = {}) => {
  return service({
    url,
    method: 'post',
    data
  })
}

export const put = (url, data = {}) => {
  return service({
    url,
    method: 'put',
    data
  })
}

export const del = (url, params = {}) => {
  return service({
    url,
    method: 'delete',
    params
  })
}

export { config }
