export type ApiResponse<T> = {
  code: number
  msg: string
  data: T
}

export type PageResult<T> = {
  list: T[]
  page: number
  size: number
  total: number
}
