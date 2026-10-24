// 类型化 HTTP 客户端：保持现有业务状态码和 Cookie 认证协议。
import axios, { type AxiosRequestConfig } from 'axios'

interface ApiResponse {
  code: number
  msg?: string
  data: unknown
}

export class ApiError extends Error {
  constructor(message: string, readonly code: number) { super(message); this.name = 'ApiError' }
}

/** 校验响应封装，避免把 HTML 登录页误识别为业务数据。 */
function isApiResponse(value: unknown): value is ApiResponse {
  return !!value && typeof value === 'object' && 'code' in value && typeof value.code === 'number'
}

/** 创建可独立测试的客户端，注入界面错误提示和过期处理。 */
export function createHttpClient(onExpired: () => void, onError: (message: string) => void) {
  const transport = axios.create({ baseURL: '', timeout: 5000, withCredentials: true })

  async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
    let payload: unknown
    try { payload = (await transport.request<unknown>(config)).data }
    catch (error) {
      if (axios.isCancel(error)) throw error
      if (axios.isAxiosError(error) && error.response?.status === 401) onExpired()
      const body: unknown = axios.isAxiosError(error) ? error.response?.data : undefined
      const message = isApiResponse(body) && typeof body.msg === 'string'
        ? body.msg : error instanceof Error ? error.message : '网络错误'
      onError(message)
      throw error
    }
    if (!isApiResponse(payload)) {
      const error = new ApiError('服务器响应格式不正确，请重新登录或稍后重试', 0)
      onError(error.message)
      throw error
    }
    if (payload.code !== 2000) {
      if (payload.code === 4012 || payload.code === 4013) onExpired()
      const message = payload.code === 4091 && config.url?.includes('/api/course')
        ? '课程编号已存在，请换一个课程编号'
        : (typeof payload.msg === 'string' ? payload.msg : '请求失败')
      onError(message)
      throw new ApiError(message, payload.code)
    }
    return payload.data as T
  }

  return Object.assign(request, {
    get: <T = unknown>(url: string, config?: AxiosRequestConfig) => request<T>({ ...config, url, method: 'get' }),
    post: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => request<T>({ ...config, url, data, method: 'post' }),
    put: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => request<T>({ ...config, url, data, method: 'put' }),
    delete: <T = unknown>(url: string, config?: AxiosRequestConfig) => request<T>({ ...config, url, method: 'delete' }),
  })
}
