import type { InjectionKey, Ref } from 'vue'
import type { LearningContext } from '@/types/learning'

export const learningContextKey: InjectionKey<Ref<LearningContext | null>> = Symbol('learning-context')
