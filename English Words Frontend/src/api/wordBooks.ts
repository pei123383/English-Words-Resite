import { http, unwrap } from '@/api/http'
import type { WordBook } from '@/types/api'

export interface WordBookPayload {
  name: string
  description?: string
}

export const wordBookApi = {
  list: () => unwrap<WordBook[]>(http.get('/word-books')),
  create: (payload: WordBookPayload) => unwrap<WordBook>(http.post('/word-books', payload)),
  update: (id: number, payload: WordBookPayload) => unwrap<WordBook>(http.put(`/word-books/${id}`, payload)),
  remove: (id: number) => unwrap<void>(http.delete(`/word-books/${id}`))
}
