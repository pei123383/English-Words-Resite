import { http, unwrap } from '@/api/http'
import type { DictionaryEntry, DictionaryQuery, PageResponse } from '@/types/api'

export const dictionaryApi = {
  search: (params: DictionaryQuery) => unwrap<PageResponse<DictionaryEntry>>(http.get('/dictionary', { params }))
}
