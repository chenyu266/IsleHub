import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['islehub-token'] = token
  }
  return config
})

request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      if (!response.config.silent) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    const status = error.response?.status
    const resData = error.response?.data
    const silent = error.config?.silent

    if (status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }

    if (!silent) {
      // 优先取后端返回的 message，其次按状态码兜底
      const msg = resData?.message
        || (status === 403 ? '无访问权限'
          : status === 404 ? '请求的资源不存在'
          : status === 500 ? '服务器内部错误'
          : '网络错误，请稍后重试')

      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
