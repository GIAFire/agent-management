import { post } from '@/axios/request'

export const login = (data) => {
  return post('/auth/auth/login', data)
}

