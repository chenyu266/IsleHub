import request from './request'
export const getAddresses = () => request.get('/address')
export const addAddress = (data) => request.post('/address', data)
export const updateAddress = (id, data) => request.put(`/address/${id}`, data)
export const deleteAddress = (id) => request.delete(`/address/${id}`)
