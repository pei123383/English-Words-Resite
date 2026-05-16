import { http, unwrap } from '@/api/http'
import type { QuizMode, QuizQuestion, QuizRecord, QuizSubmitResult } from '@/types/api'

export interface QuizRandomParams {
  bookId?: number
  count: number
  mode: QuizMode
  onlyDue: boolean
}

export interface QuizSubmitPayload {
  wordId: number
  mode: QuizMode
  selectedAnswer: string
  responseTimeMs: number
}

export const quizApi = {
  random: (params: QuizRandomParams) => unwrap<QuizQuestion[]>(http.get('/quizzes/random', { params })),
  submit: (payload: QuizSubmitPayload) => unwrap<QuizSubmitResult>(http.post('/quizzes/submit', payload)),
  history: (limit = 50) => unwrap<QuizRecord[]>(http.get('/quizzes/history', { params: { limit } }))
}
