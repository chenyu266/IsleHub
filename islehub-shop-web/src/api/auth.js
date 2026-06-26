import request from './request'
import userRequest from './userRequest'

// 登录 / 获取信息 → 复用管理后台 /api/user 接口，避免重复
export const login = (data) => userRequest.post('/login', data)
export const getInfo = () => userRequest.get('/info')

// 注册 → 复用 /api/user/register 接口
export const register = (data) => userRequest.post('/register', data)

// 发送邮箱验证码
export const sendEmailCode = (email) => userRequest.post('/send-email-code', null, { params: { email } })
