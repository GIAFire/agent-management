import axios from 'axios'
import { ElMessage } from 'element-plus'
import config from '@/axios/axiosConfig'
import { clearAuth, getToken, getTokenType, saveLoginSession } from '@/utils/auth'

export const BASE_URL = config.baseURL

const service = axios.create({
  baseURL: config.baseURL,
  timeout: config.timeout
})

service.interceptors.request.use(
  (requestConfig) => {
    const token = getToken()
    if (token) {
      requestConfig.headers = requestConfig.headers || {}
      requestConfig.headers.Authorization = `${getTokenType()} ${token}`
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

      if (response.config?.url?.includes('/auth/login')) {
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
