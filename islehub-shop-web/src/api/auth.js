import userRequest from './userRequest'

// 登录 / 获取信息 → 复用管理后台 /api/user 接口，避免重复
export const login = (data = {}) => userRequest.post('/login', {
  account: data.account ?? data.username,
  password: data.password
})
export const getInfo = () => userRequest.get('/info')

export const updateUsername = (username) => userRequest.put('/username', { username })

export const updatePassword = (data) => userRequest.put('/password', data)

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return userRequest.post('/avatar', formData)
}

export const sendChangeEmailCode = () => userRequest.post('/email/change')

export const verifyOldEmailCode = (oldCode) => userRequest.post('/email/verify', { oldCode })

export const sendNewChangeEmailCode = (newEmail) => userRequest.post('/email/new', { newEmail })

export const confirmChangeEmail = (newCode) => userRequest.put('/email', { newCode })

// 注册 → 复用 /api/user/register 接口
export const register = (data) => userRequest.post('/register', data)

// 发送邮箱验证码
export const sendEmailCode = (email) => userRequest.post('/send-email-code', null, { params: { email } })
