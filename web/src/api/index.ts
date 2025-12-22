import client from './client'
import type { PageResult } from './types'

export type Department = {
  id: number
  deptCode: string
  deptName: string
  remark?: string
}

export type LocationSpace = {
  id: number
  deptId: number
  deptName?: string
  roomNo: string
  area: number
  remark?: string
}

export type Assignee = {
  id: number
  empNo: string
  name: string
  phone?: string
  remark?: string
}

export type Asset = {
  id: number
  assetNo: string
  assetName: string
  value: number
  locationId: number
  roomNo?: string
  deptId?: number
  deptName?: string
  assigneeId: number | null
  assigneeName?: string | null
  status: number
  remark?: string
}

export type PageQuery = {
  page: number
  size: number
  keyword?: string
}

export const fetchDepartments = (params: PageQuery) =>
  client.get<PageResult<Department>>('/departments', { params })

export const createDepartment = (payload: Omit<Department, 'id'>) =>
  client.post('/departments', payload)

export const updateDepartment = (id: number, payload: Omit<Department, 'id'>) =>
  client.put(`/departments/${id}`, payload)

export const deleteDepartment = (id: number) => client.delete(`/departments/${id}`)

export const fetchLocations = (params: PageQuery & { deptId?: number }) =>
  client.get<PageResult<LocationSpace>>('/locations', { params })

export const createLocation = (payload: Omit<LocationSpace, 'id' | 'deptName'>) =>
  client.post('/locations', payload)

export const updateLocation = (id: number, payload: Omit<LocationSpace, 'id' | 'deptName'>) =>
  client.put(`/locations/${id}`, payload)

export const deleteLocation = (id: number) => client.delete(`/locations/${id}`)

export const fetchDepartmentLocations = (deptId: number) =>
  client.get<Array<{ id: number; roomNo: string }>>(`/departments/${deptId}/locations`)

export const fetchAssignees = (params: PageQuery) =>
  client.get<PageResult<Assignee>>('/assignees', { params })

export const createAssignee = (payload: Omit<Assignee, 'id'>) => client.post('/assignees', payload)

export const updateAssignee = (id: number, payload: Omit<Assignee, 'id'>) =>
  client.put(`/assignees/${id}`, payload)

export const deleteAssignee = (id: number) => client.delete(`/assignees/${id}`)

export const fetchAssigneeAssets = (id: number, params: PageQuery) =>
  client.get<PageResult<Asset>>(`/assignees/${id}/assets`, { params })

export const fetchAssets = (
  params: PageQuery & {
    deptId?: number
    locationId?: number
    assigneeId?: number
    status?: number
  },
) => client.get<PageResult<Asset>>('/assets', { params })

export const createAsset = (payload: Omit<Asset, 'id' | 'deptName' | 'roomNo' | 'assigneeName'>) =>
  client.post('/assets', payload)

export const updateAsset = (id: number, payload: Omit<Asset, 'id' | 'deptName' | 'roomNo' | 'assigneeName'>) =>
  client.put(`/assets/${id}`, payload)

export const deleteAsset = (id: number) => client.delete(`/assets/${id}`)

export const assignAsset = (id: number, assigneeId: number) =>
  client.post(`/assets/${id}/assign`, { assigneeId })

export const returnAsset = (id: number) => client.post(`/assets/${id}/return`)

export const checkHealth = () => client.get<{ status: string }>('/health', { timeout: 2000 })
