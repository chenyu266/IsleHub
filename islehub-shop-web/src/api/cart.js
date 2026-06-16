import request from './request'
export const getCart = () => request.get('/cart')
export const addToCart = (data) => request.post('/cart', data)
export const updateQuantity = (skuId, quantity) => request.put(`/cart/${skuId}`, { quantity })
export const removeFromCart = (skuId) => request.delete(`/cart/${skuId}`)
export const clearCart = () => request.delete('/cart')
