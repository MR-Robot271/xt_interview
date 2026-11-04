import request from '@/utils/request'

// 认证相关 API
export const authApi = {
  login(data: { username: string; password: string }) {
    return request.post('/auth/login', data)
  },
  logout() {
    return request.post('/auth/logout')
  },
  validate() {
    return request.get('/auth/validate')
  }
}

// 账号管理 API
export const accountApi = {
  getAll() {
    return request.get('/accounts')
  },
  getById(id: number) {
    return request.get(`/accounts/${id}`)
  },
  create(data: any) {
    return request.post('/accounts', data)
  },
  update(id: number, data: any) {
    return request.put(`/accounts/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/accounts/${id}`)
  }
}

// 部门管理 API
export const departmentApi = {
  getAll() {
    return request.get('/departments')
  },
  getById(id: number) {
    return request.get(`/departments/${id}`)
  },
  create(data: any) {
    return request.post('/departments', data)
  },
  update(id: number, data: any) {
    return request.put(`/departments/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/departments/${id}`)
  }
}
