package com.englishwords.service;

import com.englishwords.common.BusinessException;
import com.englishwords.dto.progress.ProgressResponse;
import com.englishwords.dto.quiz.QuizQuestionResponse;
import com.englishwords.dto.quiz.QuizRecordResponse;
import com.englishwords.dto.quiz.QuizSubmitRequest;
import com.englishwords.dto.quiz.QuizSubmitResponse;
import com.englishwords.entity.QuizRecord;
import com.englishwords.entity.User;
import com.englishwords.entity.Word;
import com.englishwords.entity.WordProgress;
import com.englishwords.enums.QuizMode;
import com.englishwords.repository.QuizRecordRepository;
import com.englishwords.repository.UserRepository;
import com.englishwords.repository.WordProgressRepository;
import com.englishwords.repository.WordRepository;
import com.englishwords.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final WordRepository wordRepository;
    private final WordProgressRepository progressRepository;
    private final QuizRecordRepository quizRecordRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MapperService mapperService;
    private final Sm2Service sm2Service;

    public QuizService(
        WordRepository wordRepository,
        WordProgressRepository progressRepository,
        QuizRecordRepository quizRecordRepository,
        UserRepository userRepository,
        CurrentUserProvider currentUserProvider,
        MapperService mapperService,
        Sm2Service sm2Service
    ) {
        this.wordRepository = wordRepository;
        this.progressRepository = progressRepository;
        this.quizRecordRepository = quizRecordRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.mapperService = mapperService;
        this.sm2Service = sm2Service;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> random(Long bookId, int count, QuizMode mode, boolean onlyDue) {
        Long userId = currentUserProvider.userId();
        int safeCount = Math.max(1, Math.min(count, 100));
        QuizMode requestedMode = mode == null ? QuizMode.MIXED : mode;
        return wordRepository.findRandomWords(
                userId,
                bookId,
                onlyDue,
                PageRequest.of(0, safeCount)
            )
            .stream()
            .map(word -> toMultipleChoiceQuestion(userId, word, chooseMode(requestedMode)))
            .filter(question -> question.options().size() >= 2)
            .toList();
    }

    @Transactional
    public QuizSubmitResponse submit(QuizSubmitRequest request) {
        Long userId = currentUserProvider.userId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        Word word = wordRepository.findByIdAndUserId(request.wordId(), userId)
            .orElseThrow(() -> new BusinessException("单词不存在或无权限访问"));
        WordProgress progress = progressRepository.findByUserIdAndWordId(userId, word.getId())
            .orElseGet(() -> {
                WordProgress created = new WordProgress();
                created.setUser(user);
                created.setWord(word);
                return progressRepository.save(created);
            });

        String correctAnswer = answerFor(word, request.mode());
        boolean correct = isCorrectAnswer(request.selectedAnswer(), correctAnswer, request.mode());
        int quality = calculateQuality(correct, request.responseTimeMs());
        sm2Service.applyReview(progress, quality, LocalDateTime.now());

        QuizRecord record = new QuizRecord();
        record.setUser(user);
        record.setWord(word);
        record.setMode(request.mode());
        record.setQuality(quality);
        record.setRemembered(correct);
        quizRecordRepository.save(record);

        return new QuizSubmitResponse(
            correct,
            request.selectedAnswer().trim(),
            correctAnswer,
            quality,
            request.responseTimeMs(),
            mapperService.toProgressResponse(progress)
        );
    }

    @Transactional(readOnly = true)
    public List<QuizRecordResponse> history(int limit) {
        Long userId = currentUserProvider.userId();
        return quizRecordRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, Math.max(1, Math.min(limit, 100)))
            )
            .stream()
            .map(mapperService::toQuizRecordResponse)
            .toList();
    }

    private QuizQuestionResponse toMultipleChoiceQuestion(Long userId, Word word, QuizMode mode) {
        String answer = answerFor(word, mode);
        Set<String> options = new LinkedHashSet<>();
        addOption(options, answer);
        addDistractors(options, wordRepository.findByUserIdAndWordBookIdAndIdNot(
            userId,
            word.getWordBook().getId(),
            word.getId()
        ), mode);
        if (options.size() < 4) {
            addDistractors(options, wordRepository.findByUserIdAndWordBookIdNotAndIdNot(
                userId,
                word.getWordBook().getId(),
                word.getId()
            ), mode);
        }

        List<String> shuffledOptions = new ArrayList<>(options);
        Collections.shuffle(shuffledOptions);
        return mapperService.toQuestionResponse(word, mode, shuffledOptions);
    }

    private void addDistractors(Set<String> options, List<Word> candidates, QuizMode mode) {
        List<Word> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        for (Word candidate : shuffled) {
            if (options.size() >= 4) {
                return;
            }
            addOption(options, answerFor(candidate, mode));
        }
    }

    private void addOption(Set<String> options, String value) {
        if (value != null && !value.trim().isEmpty()) {
            options.add(value.trim());
        }
    }

    private String answerFor(Word word, QuizMode mode) {
        return mode == QuizMode.EN_TO_CN ? word.getTranslation() : word.getTerm();
    }

    private boolean isCorrectAnswer(String selectedAnswer, String correctAnswer, QuizMode mode) {
        String selected = selectedAnswer == null ? "" : selectedAnswer.trim();
        String correct = correctAnswer == null ? "" : correctAnswer.trim();
        if (mode == QuizMode.CN_TO_EN) {
            return selected.toLowerCase(Locale.ROOT).equals(correct.toLowerCase(Locale.ROOT));
        }
        return selected.equals(correct);
    }

    private int calculateQuality(boolean correct, long responseTimeMs) {
        if (correct) {
            if (responseTimeMs <= 5_000) {
                return 5;
            }
            if (responseTimeMs <= 15_000) {
                return 4;
            }
            return 3;
        }
        if (responseTimeMs <= 5_000) {
            return 2;
        }
        if (responseTimeMs <= 15_000) {
            return 1;
        }
        return 0;
    }

    private QuizMode chooseMode(QuizMode mode) {
        if (mode != QuizMode.MIXED) {
            return mode;
        }
        return ThreadLocalRandom.current().nextBoolean() ? QuizMode.EN_TO_CN : QuizMode.CN_TO_EN;
    }
}
