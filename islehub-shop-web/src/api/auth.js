import request from './request'
import userRequest from './userRequest'

// 登录 / 获取信息 → 复用管理后台 /api/user 接口，避免重复
export const login = (data) => userRequest.post('/login', data)
export const getInfo = () => userRequest.get('/info')

// 注册 → 商城专属逻辑（设置 role=customer）
export const register = (data) => request.post('/auth/register', data)
