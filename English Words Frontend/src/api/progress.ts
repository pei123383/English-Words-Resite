import { http, unwrap } from '@/api/http'
import type { Progress, ProgressOverview } from '@/types/api'

export const progressApi = {
  overview: () => unwrap<ProgressOverview>(http.get('/progress/overview')),
  due: (limit = 50) => unwrap<Progress[]>(http.get('/progress/due', { params: { limit } })),
  reset: (wordId: number) => unwrap<Progress>(http.put(`/progress/${wordId}/reset`))
}
