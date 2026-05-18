export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface User {
  id: number
  username: string
  nickname: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface WordBook {
  id: number
  name: string
  description: string | null
  wordCount: number
  createdAt: string
  updatedAt: string
}

export interface Word {
  id: number
  bookId: number
  bookName: string
  word: string
  translation: string
  phonetic: string | null
  example: string | null
  tags: string | null
  reviewCount: number
  correctCount: number
  wrongCount: number
  easeFactor: number
  intervalDays: number
  repetition: number
  masteryLevel: number
  nextReviewAt: string | null
  lastReviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface WordPayload {
  bookId: number
  word: string
  translation: string
  phonetic?: string
  example?: string
  tags?: string
}

export interface WordQuery {
  bookId?: number
  keyword?: string
  tag?: string
  masteryLevel?: number
  onlyDue?: boolean
  page?: number
  size?: number
}

export interface WordImportResult {
  totalRows: number
  createdCount: number
  updatedCount: number
  skippedCount: number
  errors: string[]
}

export interface DictionaryEntry {
  word: string
  translation: string
}

export interface DictionaryQuery {
  keyword?: string
  page?: number
  size?: number
}


export type QuizMode = 'EN_TO_CN' | 'CN_TO_EN' | 'MIXED'

export interface QuizQuestion {
  wordId: number
  bookId: number
  mode: QuizMode
  prompt: string
  answer: string
  options: string[]
  word: string
  translation: string
  phonetic: string | null
  example: string | null
  tags: string | null
  masteryLevel: number
}

export interface QuizSubmitResult {
  correct: boolean
  selectedAnswer: string
  correctAnswer: string
  quality: number
  responseTimeMs: number
  progress: Progress
}

export interface QuizRecord {
  id: number
  wordId: number
  word: string
  translation: string
  mode: QuizMode
  quality: number
  remembered: boolean
  createdAt: string
}

export interface ProgressOverview {
  totalWords: number
  dueWords: number
  masteredWords: number
  totalReviews: number
}

export interface Progress {
  wordId: number
  word: string
  translation: string
  reviewCount: number
  correctCount: number
  wrongCount: number
  easeFactor: number
  intervalDays: number
  repetition: number
  masteryLevel: number
  nextReviewAt: string | null
  lastReviewedAt: string | null
}
