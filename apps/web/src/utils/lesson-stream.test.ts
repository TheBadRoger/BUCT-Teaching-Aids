// 网络分块不能作为 SSE 事件或 UTF-8 字符的边界。
import { describe, expect, it, vi } from 'vitest'
import { readLessonStream } from './lesson-stream'

function stream(parts: (string | Uint8Array)[]) {
  return new ReadableStream<Uint8Array>({ start(controller) {
    for (const part of parts) controller.enqueue(typeof part === 'string' ? new TextEncoder().encode(part) : part)
    controller.close()
  } })
}

describe('lesson SSE', () => {
  it('reassembles UTF-8 and JSON split at every byte', async () => {
    const bytes = new TextEncoder().encode('data: {"type":"chunk","content":"教学目标：浮力"}\n\ndata: {"type":"done"}\n\n')
    const received: string[] = []
    await readLessonStream(stream(Array.from(bytes, byte => new Uint8Array([byte]))), text => received.push(text), new AbortController().signal)
    expect(received.join('')).toBe('教学目标：浮力')
  })

  it('accepts CRLF, heartbeats, multiline data and a final frame without delimiter', async () => {
    const received = vi.fn()
    await readLessonStream(stream([': heartbeat\r\n\r\ndata: {"type":"chunk",\r\ndata: "content":"内容"}\r\n\r', '\ndata: {"type":"done"}']), received, new AbortController().signal)
    expect(received).toHaveBeenCalledExactlyOnceWith('内容')
  })

  it('does not treat a truncated stream as a successful generation', async () => {
    await expect(readLessonStream(stream(['data: {"type":"chunk","content":"半段内容"}\n\n']), vi.fn(), new AbortController().signal)).rejects.toThrow('提前中断')
  })

  it('reports the error event emitted by the existing service', async () => {
    await expect(readLessonStream(stream(['data: {"type":"error","message":"模型额度不足"}\n\n']), vi.fn(), new AbortController().signal)).rejects.toThrow('模型额度不足')
  })

  it.each(['data: {broken}\n\n', 'data: {"type":"unknown"}\n\n', 'data: {}\n\n'])('rejects malformed events', async frame => {
    await expect(readLessonStream(stream([frame]), vi.fn(), new AbortController().signal)).rejects.toThrow()
  })

  it('cancels even when done is already buffered after the first chunk', async () => {
    const controller = new AbortController()
    await expect(readLessonStream(stream(['data: {"type":"chunk","content":"部分"}\n\ndata: {"type":"done"}\n\n']), () => controller.abort(), controller.signal)).rejects.toMatchObject({ name: 'AbortError' })
  })

  it('rejects an unbounded incomplete event', async () => {
    await expect(readLessonStream(stream(['data: ' + 'x'.repeat(1024 * 1024)]), vi.fn(), new AbortController().signal)).rejects.toThrow('过大')
  })
})
