export const config = {
  // 本地开发：走 Vite 代理 /fishingMan -> http://localhost:8080
  development: {
    baseURL: '',
    timeout: 10000
  },
  // 生产：走 Nginx 等代理，或直接配网关地址
  production: {
    baseURL: '',
    timeout: 10000
  },
  // 微信小程序开发：直接请求网关
  xiaoChengXuDev: {
    baseURL: 'http://localhost:8080',
    timeout: 10000
  }
}

// 适配微信小程序环境
let env = 'development'
try {
  // 微信小程序环境
  if (typeof wx !== 'undefined' && wx.getAccountInfoSync) {
    const accountInfo = wx.getAccountInfoSync()
    env = accountInfo.miniProgram.envVersion === 'release' ? 'production' : 'xiaoChengXuDev'
  } else if (typeof process !== 'undefined' && process.env) {
    // Node.js环境
    env = process.env.NODE_ENV || 'development'
  }
} catch (e) {
  // 其他环境
  env = 'development'
}

export default config[env]
