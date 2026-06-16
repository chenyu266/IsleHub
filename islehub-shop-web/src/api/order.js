import request from './request'
export const checkout = (data) => request.post('/order/checkout', data)
export const pageOrders = (params) => request.get('/order/page', { params })
export const getOrder = (id) => request.get(`/order/${id}`)
export const cancelOrder = (id) => request.put(`/order/${id}/cancel`)
export const confirmOrder = (id) => request.put(`/order/${id}/confirm`)
