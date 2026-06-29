import request from './request'

export const login = (data = {}) => request.post('/user/login', {
  account: data.account ?? data.username,
  password: data.password
})
export const getInfo = () => request.get('/user/info')
export const pageUsers = (params, config) => request.get('/user/page', { params, ...config })
export const addUser = (data) => request.post('/user', data)
export const updateUser = (id, data) => request.put(`/user/${id}`, data)
export const deleteUser = (id) => request.delete(`/user/${id}`)
export const updateUserStatus = (id, status) => request.put(`/user/${id}/status`, null, { params: { status } })
