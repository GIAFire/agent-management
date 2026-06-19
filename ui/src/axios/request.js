import config from '@/axios/axiosConfig' // 导入 axios 配置文件
import { useUserStore } from '@/stores/user.js'
import {request} from "axios"; // 导入用户状态管理 store

const userStore = useUserStore()
export const BASE_URL = config.baseURL // 导出基础 URL 常量
const TIMEOUT = config.timeout // 定义请求超时常量

export const get = (url, data = {}) => { // 导出 GET 请求方法
	return request(url, 'GET', data) // 调用通用请求函数，方法为 GET
}

export const post = (url, data = {}) => { // 导出 POST 请求方法
	return request(url, 'POST', data) // 调用通用请求函数，方法为 POST
}

export const put = (url, data = {}) => { // 导出 PUT 请求方法
	return request(url, 'PUT', data) // 调用通用请求函数，方法为 PUT
}

export const del = (url, data = {}) => { // 导出 DELETE 请求方法
	return request(url, 'DELETE', data) // 调用通用请求函数，方法为 DELETE
}

export { config } // 导出配置对象
