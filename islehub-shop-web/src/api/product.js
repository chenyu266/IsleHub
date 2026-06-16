import request from './request'
export const pageProducts = (params) => request.get('/product/page', { params })
export const getProduct = (id) => request.get(`/product/${id}`)
export const getCategoryTree = () => request.get('/category/tree')
