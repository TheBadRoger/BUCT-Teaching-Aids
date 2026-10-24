import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useFormSubmit } from './useFormSubmit'

describe('form submission', () => {
  it('blocks a second click while asynchronous validation is pending', async () => {
    let validate!: (valid: boolean) => void
    const form = ref({ validate: vi.fn(() => new Promise<boolean>(resolve => { validate = resolve })) })
    const state = useFormSubmit(form)
    const save = vi.fn(async () => {})
    const pending = state.submit(save)
    expect(state.submitting.value).toBe(true)
    await expect(state.submit(save)).resolves.toBe(false)
    expect(form.value.validate).toHaveBeenCalledOnce()
    validate(true)
    await expect(pending).resolves.toBe(true)
    expect(save).toHaveBeenCalledOnce()
    expect(state.submitting.value).toBe(false)
  })

  it('does not send invalid forms', async () => {
    const state = useFormSubmit(ref({ validate: vi.fn().mockRejectedValue({ name: ['required'] }) }))
    const save = vi.fn()
    await expect(state.submit(save)).resolves.toBe(false)
    expect(save).not.toHaveBeenCalled()
    expect(state.error.value).toBe('')
    expect(state.submitting.value).toBe(false)
  })

  it('keeps input after failure, displays its cause, and allows retry', async () => {
    const input = ref('保留输入')
    const state = useFormSubmit(ref({ validate: vi.fn().mockResolvedValue(true) }))
    const save = vi.fn().mockRejectedValueOnce(new Error('用户名已存在')).mockResolvedValueOnce(undefined)
    await expect(state.submit(() => save(input.value))).resolves.toBe(false)
    expect(state.error.value).toBe('用户名已存在')
    expect(input.value).toBe('保留输入')
    await expect(state.submit(() => save(input.value))).resolves.toBe(true)
    expect(state.error.value).toBe('')
    expect(save).toHaveBeenLastCalledWith('保留输入')
  })
})
