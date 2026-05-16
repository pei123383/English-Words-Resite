import { http, unwrap } from '@/api/http'
import type { PageResponse, Word, WordImportResult, WordPayload, WordQuery } from '@/types/api'

export const wordsApi = {
  list: (params: WordQuery) => unwrap<PageResponse<Word>>(http.get('/words', { params })),
  create: (payload: WordPayload) => unwrap<Word>(http.post('/words', payload)),
  update: (id: number, payload: WordPayload) => unwrap<Word>(http.put(`/words/${id}`, payload)),
  remove: (id: number) => unwrap<void>(http.delete(`/words/${id}`)),
  import: (bookId: number, file: File) => {
    const formData = new FormData()
    formData.append('bookId', String(bookId))
    formData.append('file', file)
    return unwrap<WordImportResult>(
      http.post('/words/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    )
  }
}
