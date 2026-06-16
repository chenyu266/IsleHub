import request from './request'

export const pageOrders = (params, config) => request.get('/order/page', { params, ...config })
export const getOrder = (id) => request.get(`/order/${id}`)
export const createOrder = (data) => request.post('/order', data)
export const updateOrderStatus = (id, data) => request.put(`/order/${id}/status`, data)
export const cancelOrder = (id) => request.put(`/order/${id}/cancel`)
export const addShipping = (id, data) => request.post(`/order/${id}/shipping`, data)
export const exportOrders = (params) => request.get('/order/export', { params, responseType: 'blob' })
