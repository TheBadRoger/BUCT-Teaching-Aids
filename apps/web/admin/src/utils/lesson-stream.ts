// 处理现有 Express 教案接口的 JSON 型 SSE，兼容任意网络分块。

/** 读取完整事件，只有收到 done 才视为成功；断流、错误及取消向调用方抛出。 */
export async function readLessonStream(body: ReadableStream<Uint8Array>, onChunk: (text: string) => void, signal: AbortSignal): Promise<void> {
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let complete = false
  const maxBufferLength = 1024 * 1024
  const abort = () => { void reader.cancel().catch(() => undefined) }
  signal.addEventListener('abort', abort, { once: true })

  /** SSE 注释与非 data 字段不属于模型输出。 */
  function process(frame: string) {
    const data = frame.split(/\r?\n/).filter(line => line.startsWith('data:')).map(line => line.slice(5).replace(/^ /, '')).join('\n')
    if (!data) return
    let value: unknown
    try { value = JSON.parse(data) } catch { throw new Error('生成流响应格式不正确，请重试') }
    if (!value || typeof value !== 'object' || !('type' in value)) throw new Error('生成流缺少事件类型')
    if (value.type === 'chunk' && 'content' in value && typeof value.content === 'string') onChunk(value.content)
    else if (value.type === 'done') complete = true
    else if (value.type === 'error') throw new Error('message' in value && typeof value.message === 'string' ? value.message : 'AI 生成失败')
    else throw new Error('生成流事件不受支持')
  }

  try {
    while (!complete) {
      signal.throwIfAborted()
      const { value, done } = await reader.read()
      signal.throwIfAborted()
      buffer += decoder.decode(value, { stream: !done })
      if (buffer.length > maxBufferLength) throw new Error('生成流单个事件过大，请重试')
      let boundary = /\r?\n\r?\n/.exec(buffer)
      while (boundary && !complete) {
        signal.throwIfAborted()
        process(buffer.slice(0, boundary.index))
        buffer = buffer.slice(boundary.index + boundary[0].length)
        boundary = /\r?\n\r?\n/.exec(buffer)
      }
      if (done) {
        if (!complete && buffer.trim()) process(buffer)
        if (!complete) throw new Error('生成连接提前中断，请重试')
        break
      }
    }
    signal.throwIfAborted()
  } finally {
    signal.removeEventListener('abort', abort)
    await reader.cancel().catch(() => undefined)
    reader.releaseLock()
  }
}
