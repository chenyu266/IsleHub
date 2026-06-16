import request from './request'
import axios from 'axios'

export const pageProducts = (params, config) => request.get('/product/page', { params, ...config })
export const getProduct = (id) => request.get(`/product/${id}`)
export const addProduct = (data) => request.post('/product', data)
export const updateProduct = (id, data) => request.put(`/product/${id}`, data)
export const deleteProduct = (id) => request.delete(`/product/${id}`)
export const updateProductStatus = (id, status) => request.put(`/product/${id}/status`, null, { params: { status } })
export const batchProductStatus = (data) => request.put('/product/batch-status', data)
export const getCategoryTree = () => request.get('/category/tree')
export const addCategory = (data) => request.post('/category', data)
export const updateCategory = (id, data) => request.put(`/category/${id}`, data)
export const deleteCategory = (id) => request.delete(`/category/${id}`)

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const token = localStorage.getItem('token')
  return axios.post('/api/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...(token ? { 'islehub-token': token } : {})
    }
  }).then(res => res.data)
}
