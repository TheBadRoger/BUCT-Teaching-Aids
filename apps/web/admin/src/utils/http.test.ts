// 验证业务响应、登录过期和非 JSON 响应的边界行为。
import { describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, CanceledError, type AxiosAdapter } from 'axios'
import { ApiError, createHttpClient } from './http'

function response(data: unknown): AxiosAdapter {
  return async config => ({ data, status: 200, statusText: 'OK', headers: new AxiosHeaders(), config })
}

describe('typed HTTP client', () => {
  it('does not show an error or expire login when a previous query is canceled', async () => {
    const expired = vi.fn()
    const notify = vi.fn()
    const request = createHttpClient(expired, notify)
    const adapter: AxiosAdapter = async () => { throw new CanceledError() }
    await expect(request.get('/api/teacher/search', { adapter })).rejects.toBeInstanceOf(CanceledError)
    expect(notify).not.toHaveBeenCalled()
    expect(expired).not.toHaveBeenCalled()
  })
  it('unwraps successful data without changing the Cookie transport', async () => {
    const request = createHttpClient(vi.fn(), vi.fn())
    const adapter: AxiosAdapter = async config => {
      expect(config.withCredentials).toBe(true)
      return response({ code: 2000, data: { content: [] } })(config)
    }
    await expect(request.get('/api/teacher/search', { adapter })).resolves.toEqual({ content: [] })
  })

  it.each([4012, 4013])('expires the session on business code %i', async code => {
    const expired = vi.fn()
    const request = createHttpClient(expired, vi.fn())
    await expect(request.get('/api/teacher/search', { adapter: response({ code, msg: 'expired' }) })).rejects.toBeInstanceOf(ApiError)
    expect(expired).toHaveBeenCalledOnce()
  })

  it('handles an HTTP 401 as session expiry', async () => {
    const expired = vi.fn()
    const request = createHttpClient(expired, vi.fn())
    const adapter: AxiosAdapter = async config => { throw new AxiosError('Unauthorized', '401', config, undefined, { status: 401, statusText: 'Unauthorized', data: {}, headers: new AxiosHeaders(), config }) }
    await expect(request.get('/api/teacher/search', { adapter })).rejects.toThrow('Unauthorized')
    expect(expired).toHaveBeenCalledOnce()
  })

  it('rejects an HTML login response rather than returning undefined data', async () => {
    const notify = vi.fn()
    const request = createHttpClient(vi.fn(), notify)
    await expect(request.get('/api/teacher/search', { adapter: response('<html>login</html>') })).rejects.toBeInstanceOf(ApiError)
    expect(notify).toHaveBeenCalledOnce()
  })

  it('keeps the course-number conflict message', async () => {
    const request = createHttpClient(vi.fn(), vi.fn())
    await expect(request.post('/api/course/add', {}, { adapter: response({ code: 4091 }) })).rejects.toThrow('课程编号已存在')
  })
})
