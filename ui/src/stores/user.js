import { defineStore } from 'pinia' // 导入 Pinia 的 defineStore 函数，用于创建状态管理 store
import { ref, computed } from 'vue' // 导入 Vue 的 ref 和 computed 响应式 API

export const useUserStore = defineStore('user', () => { // 定义并导出名为 'user' 的 store，使用组合式 API 风格
	// 用户信息状态
	const userInfo = ref(null) // 创建响应式引用，存储用户信息对象，初始值为 null
	// 登录令牌
	const token = ref('') // 创建响应式引用，存储访问令牌，初始值为空字符串
	// 登录状态
	const isLoggedIn = ref(false) // 创建响应式引用，存储登录状态，初始值为 false

	// 获取用户ID
	const getUserId = computed(() => userInfo.value?.id || '') // 计算属性，从用户信息中获取 ID，不存在则返回空字符串
	// 获取用户昵称
	const getNickName = computed(() => userInfo.value?.nickName || '') // 计算属性，从用户信息中获取昵称，不存在则返回空字符串
	// 获取用户头像URL
	const getAvatarUrl = computed(() => userInfo.value?.avatarUrl || '') // 计算属性，从用户信息中获取头像 URL，不存在则返回空字符串
	// 获取用户手机号
	const getPhone = computed(() => userInfo.value?.phone || '') // 计算属性，从用户信息中获取手机号，不存在则返回空字符串
	// 获取用户等级
	const getUserLevel = computed(() => userInfo.value?.userLevel || 0) // 计算属性，从用户信息中获取用户等级，不存在则返回 0
	// 获取访问令牌
	const getAccessToken = computed(() => userInfo.value?.tokenInfo?.accessToken || '') // 计算属性，从 tokenInfo 中获取访问令牌，不存在则返回空字符串
	// 获取刷新令牌
	const getAccessTokenExpires = computed(() => userInfo.value?.tokenInfo?.accessTokenExpires || 0) // 计算属性，从 tokenInfo 中获取访问令牌过期时间（秒），不存在则返回 0

	const getRefreshToken = computed(() => userInfo.value?.tokenInfo?.refreshToken || '') // 计算属性，从 tokenInfo 中获取刷新令牌，不存在则返回空字符串
	// 获取访问令牌过期时间
	// 获取刷新令牌过期时间
	const getRefreshTokenExpires = computed(() => userInfo.value?.tokenInfo?.refreshTokenExpires || 0) // 计算属性，从 tokenInfo 中获取刷新令牌过期时间（毫秒），不存在则返回 0

	// 设置登录数据
	const setLoginData = (loginResponse) => { // 定义设置登录数据的函数，接收登录响应对象
		const loginData = loginResponse.data // 从响应对象中解构出 data 字段，即实际的登录数据

		userInfo.value = loginData // 将登录数据赋值给 userInfo 响应式引用
		token.value = loginData.tokenInfo.accessToken // 从 tokenInfo 中提取访问令牌并赋值给 token
		isLoggedIn.value = true // 将登录状态设置为 true

		saveToStorage() // 调用 saveToStorage 函数，将数据保存到本地存储
	}

	// 更新 token 信息
	const updateTokenInfo = (tokenInfo) => { // 定义更新 token 信息的函数，接收新的 tokenInfo 对象
		if (userInfo.value && tokenInfo) { // 检查用户信息和 tokenInfo 是否存在
			userInfo.value.tokenInfo = { ...userInfo.value.tokenInfo, ...tokenInfo } // 合并旧的 tokenInfo 和新的 tokenInfo
			token.value = tokenInfo.accessToken // 更新 token 值，优先使用访问令牌
			saveToStorage() // 调用 saveToStorage 函数，将更新后的数据保存到本地存储
		}
	}

	// 退出登录
	const logout = () => { // 定义退出登录的函数
		userInfo.value = null // 将用户信息重置为 null
		token.value = '' // 将令牌重置为空字符串
		isLoggedIn.value = false // 将登录状态设置为 false

		clearStorage() // 调用 clearStorage 函数，清除本地存储的数据
	}

	// 检查登录状态
	const checkLoginStatus = () => { // 定义检查登录状态的函数
		const storedToken = uni.getStorageSync('token') // 从本地存储中获取 token
		const storedUserInfo = uni.getStorageSync('userInfo') // 从本地存储中获取用户信息

		if (storedToken && storedUserInfo) { // 检查 token 和用户信息是否都存在
			token.value = storedToken // 将存储的 token 赋值给响应式引用
			userInfo.value = storedUserInfo // 将存储的用户信息赋值给响应式引用
			isLoggedIn.value = true // 将登录状态设置为 true
			return true // 返回 true 表示已登录
		}
		return false // 返回 false 表示未登录
	}

	// 保存数据到本地存储
	const saveToStorage = () => { // 定义保存数据到本地存储的函数
		if (token.value) { // 检查 token 是否存在
			uni.setStorageSync('token', token.value) // 将 token 保存到本地存储
			uni.setStorageSync('tokenExpires', getAccessTokenExpires.value) // 将访问令牌过期时间保存到本地存储
			uni.setStorageSync('refreshToken', getRefreshToken.value) // 将刷新令牌保存到本地存储
			uni.setStorageSync('refreshTokenExpires', getRefreshTokenExpires.value) // 将刷新令牌过期时间保存到本地存储
			uni.setStorageSync('loginTime', userInfo.value?.loginTime) // 将登录时间保存到本地存储，不存在则使用当前时间
		}
		if (userInfo.value) { // 检查用户信息是否存在
			uni.setStorageSync('userInfo', userInfo.value) // 将用户信息保存到本地存储
		}
	}

	// 清除本地存储数据
	const clearStorage = () => { // 定义清除本地存储数据的函数
		uni.removeStorageSync('token') // 从本地存储中移除 token
		uni.removeStorageSync('userInfo') // 从本地存储中移除用户信息
		uni.removeStorageSync('tokenExpires') // 从本地存储中移除访问令牌过期时间
		uni.removeStorageSync('refreshToken') // 从本地存储中移除刷新令牌
		uni.removeStorageSync('refreshTokenExpires') // 从本地存储中移除刷新令牌过期时间
		uni.removeStorageSync('loginTime') // 从本地存储中移除登录时间
	}

	// 判断访问令牌是否已过期
	const isAccessTokenExpired = () => { // 定义判断访问令牌是否过期的函数
		const expires = getAccessTokenExpires.value // 获取访问令牌过期时间（秒）
		if (!expires) return true // 如果过期时间不存在，返回 true 表示已过期

		const now = Date.now() // 获取当前时间戳（毫秒）
		const loginTime = userInfo.value?.loginTime || 0 // 获取登录时间戳，不存在则为 0
		const expireTime = loginTime + expires // 计算过期时间戳（秒转毫秒）

		return now >= expireTime // 判断当前时间是否已超过过期时间
	}

	// 判断刷新令牌是否已过期
	const isRefreshTokenExpired = () => { // 定义判断刷新令牌是否过期的函数
		const expires = getRefreshTokenExpires.value // 获取刷新令牌过期时间（毫秒）
		if (!expires) return true // 如果过期时间不存在，返回 true 表示已过期

		const now = Date.now() // 获取当前时间戳（毫秒）
		const loginTime = userInfo.value?.loginTime || 0 // 获取登录时间戳，不存在则为 0
		const expireTime = loginTime + expires // 计算过期时间戳

		return now >= expireTime // 判断当前时间是否已超过过期时间
	}

	// 判断是否需要刷新令牌（提前5分钟）
	const shouldRefreshToken = () => { // 定义判断是否需要刷新令牌的函数
		const expires = getAccessTokenExpires.value // 获取访问令牌过期时间（秒）
		if (!expires) return false // 如果过期时间不存在，返回 false 表示不需要刷新

		const now = Date.now() // 获取当前时间戳（毫秒）
		const loginTime = userInfo.value?.loginTime || 0 // 获取登录时间戳，不存在则为 0
		const expireTime = loginTime + expires // 计算过期时间戳（秒转毫秒）

		const bufferTime = 5 * 60 * 1000 // 设置缓冲时间为 5 分钟（毫秒）
		return now >= (expireTime - bufferTime) // 判断当前时间是否已超过（过期时间 - 缓冲时间）
	}

	return { // 返回 store 的公共 API
		userInfo, // 导出用户信息响应式引用
		token, // 导出令牌响应式引用
		isLoggedIn, // 导出登录状态响应式引用
		getUserId, // 导出获取用户 ID 的计算属性
		getNickName, // 导出获取昵称的计算属性
		getAvatarUrl, // 导出获取头像 URL 的计算属性
		getPhone, // 导出获取手机号的计算属性
		getUserLevel, // 导出获取用户等级的计算属性
		getAccessToken, // 导出获取访问令牌的计算属性
		getRefreshToken, // 导出获取刷新令牌的计算属性
		getAccessTokenExpires, // 导出获取访问令牌过期时间的计算属性
		getRefreshTokenExpires, // 导出获取刷新令牌过期时间的计算属性
		setLoginData, // 导出设置登录数据的函数
		updateTokenInfo, // 导出更新 token 信息的函数
		logout, // 导出退出登录的函数
		checkLoginStatus, // 导出检查登录状态的函数
		saveToStorage, // 导出保存数据到本地存储的函数
		clearStorage, // 导出清除本地存储数据的函数
		isAccessTokenExpired, // 导出判断访问令牌是否过期的函数
		isRefreshTokenExpired, // 导出判断刷新令牌是否过期的函数
		shouldRefreshToken // 导出判断是否需要刷新令牌的函数
	}
})
