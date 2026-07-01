import axios from 'axios'
import router from '../router'

const request = axios.create({
  baseURL: '/api/shop',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('shop-token')
  if (token) {
    config.headers['islehub-token'] = token
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    const status = error.response?.status
    const resData = error.response?.data
    if (status === 401) {
      localStorage.removeItem('shop-token')
      router.push('/login')
    }

    const msg = resData?.message
      || (status === 403 ? '无访问权限'
        : status === 404 ? '请求的资源不存在'
        : status === 500 ? '服务器内部错误'
        : '网络错误，请稍后重试')

    return Promise.reject(new Error(msg))
  }
)

export default request
