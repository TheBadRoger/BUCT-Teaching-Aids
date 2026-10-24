import { describe, expect, it } from 'vitest'
import { learningMediaUrl, learningPage, lessonIndex, parseCourseLessons } from './learning'

describe('learning course input', () => {
  it('parses old outlines, strips numbering and keeps media anchored at the server root', () => {
    expect(parseCourseLessons('1. 导论|videos/intro.mp4\n2、练习；3) 讨论|/media/discussion.mp4')).toEqual([
      { title: '导论', videoUrl: '/videos/intro.mp4' }, { title: '练习', videoUrl: '' }, { title: '讨论', videoUrl: '/media/discussion.mp4' },
    ])
  })
  it.each(['javascript:alert(1)', 'data:text/html,test', '//other.test/video', 'https:\\bad.test', 'java\nscript:alert(1)'])('rejects unsafe media URL %s', value => {
    expect(learningMediaUrl(value)).toBe('')
  })
  it('preserves explicit HTTP(S) addresses and old image paths', () => {
    expect(learningMediaUrl(' https://media.test/a.mp4 ')).toBe('https://media.test/a.mp4')
    expect(learningMediaUrl('./images/test-img.jpg')).toBe('/images/test-img.jpg')
  })
  it.each([undefined, 'NaN', 'Infinity', '-1', '0', '1.5', ['2'], '1000001'])('normalizes invalid pagination %s', value => {
    expect(learningPage(value)).toBe(1)
  })
  it('clamps lesson navigation and accepts valid pagination', () => {
    expect(lessonIndex('999', 3)).toBe(2)
    expect(lessonIndex('-1', 3)).toBe(0)
    expect(lessonIndex('1.5', 3)).toBe(0)
    expect(lessonIndex('8', 0)).toBe(0)
    expect(learningPage('4')).toBe(4)
  })
})
