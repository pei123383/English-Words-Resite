package com.englishwords.service;

import com.englishwords.dto.auth.UserResponse;
import com.englishwords.dto.progress.ProgressResponse;
import com.englishwords.dto.quiz.QuizRecordResponse;
import com.englishwords.dto.quiz.QuizQuestionResponse;
import com.englishwords.dto.word.WordBookResponse;
import com.englishwords.dto.word.WordResponse;
import com.englishwords.entity.QuizRecord;
import com.englishwords.entity.User;
import com.englishwords.entity.Word;
import com.englishwords.entity.WordBook;
import com.englishwords.entity.WordProgress;
import com.englishwords.enums.QuizMode;
import com.englishwords.repository.WordBookRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MapperService {

    private final WordBookRepository wordBookRepository;

    public MapperService(WordBookRepository wordBookRepository) {
        this.wordBookRepository = wordBookRepository;
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname(), user.getCreatedAt());
    }

    public WordBookResponse toWordBookResponse(WordBook wordBook) {
        long wordCount = wordBookRepository.countWords(wordBook.getId(), wordBook.getUser().getId());
        return new WordBookResponse(
            wordBook.getId(),
            wordBook.getName(),
            wordBook.getDescription(),
            wordCount,
            wordBook.getCreatedAt(),
            wordBook.getUpdatedAt()
        );
    }

    public WordResponse toWordResponse(Word word) {
        WordProgress progress = word.getProgress();
        return new WordResponse(
            word.getId(),
            word.getWordBook().getId(),
            word.getWordBook().getName(),
            word.getTerm(),
            word.getTranslation(),
            word.getPhonetic(),
            word.getExample(),
            word.getTags(),
            progress == null ? 0 : progress.getReviewCount(),
            progress == null ? 0 : progress.getCorrectCount(),
            progress == null ? 0 : progress.getWrongCount(),
            progress == null ? 2.5 : progress.getEaseFactor(),
            progress == null ? 0 : progress.getIntervalDays(),
            progress == null ? 0 : progress.getRepetition(),
            progress == null ? 0 : progress.getMasteryLevel(),
            progress == null ? null : progress.getNextReviewAt(),
            progress == null ? null : progress.getLastReviewedAt(),
            word.getCreatedAt(),
            word.getUpdatedAt()
        );
    }

    public QuizQuestionResponse toQuestionResponse(Word word, QuizMode mode, List<String> options) {
        WordProgress progress = word.getProgress();
        QuizMode actualMode = mode == QuizMode.MIXED ? QuizMode.EN_TO_CN : mode;
        String prompt = actualMode == QuizMode.EN_TO_CN ? word.getTerm() : word.getTranslation();
        String answer = actualMode == QuizMode.EN_TO_CN ? word.getTranslation() : word.getTerm();
        return new QuizQuestionResponse(
            word.getId(),
            word.getWordBook().getId(),
            actualMode,
            prompt,
            answer,
            options,
            word.getTerm(),
            word.getTranslation(),
            word.getPhonetic(),
            word.getExample(),
            word.getTags(),
            progress == null ? 0 : progress.getMasteryLevel()
        );
    }

    public ProgressResponse toProgressResponse(WordProgress progress) {
        Word word = progress.getWord();
        return new ProgressResponse(
            word.getId(),
            word.getTerm(),
            word.getTranslation(),
            progress.getReviewCount(),
            progress.getCorrectCount(),
            progress.getWrongCount(),
            progress.getEaseFactor(),
            progress.getIntervalDays(),
            progress.getRepetition(),
            progress.getMasteryLevel(),
            progress.getNextReviewAt(),
            progress.getLastReviewedAt()
        );
    }

    public QuizRecordResponse toQuizRecordResponse(QuizRecord record) {
        return new QuizRecordResponse(
            record.getId(),
            record.getWord().getId(),
            record.getWord().getTerm(),
            record.getWord().getTranslation(),
            record.getMode(),
            record.getQuality(),
            record.isRemembered(),
            record.getCreatedAt()
        );
    }
}
