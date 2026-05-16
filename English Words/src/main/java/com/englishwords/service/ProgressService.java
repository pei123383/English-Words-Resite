package com.englishwords.service;

import com.englishwords.common.BusinessException;
import com.englishwords.dto.progress.ProgressOverviewResponse;
import com.englishwords.dto.progress.ProgressResponse;
import com.englishwords.entity.User;
import com.englishwords.entity.Word;
import com.englishwords.entity.WordProgress;
import com.englishwords.repository.UserRepository;
import com.englishwords.repository.WordProgressRepository;
import com.englishwords.repository.WordRepository;
import com.englishwords.security.CurrentUserProvider;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {

    private final WordRepository wordRepository;
    private final WordProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MapperService mapperService;
    private final Sm2Service sm2Service;

    public ProgressService(
        WordRepository wordRepository,
        WordProgressRepository progressRepository,
        UserRepository userRepository,
        CurrentUserProvider currentUserProvider,
        MapperService mapperService,
        Sm2Service sm2Service
    ) {
        this.wordRepository = wordRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.mapperService = mapperService;
        this.sm2Service = sm2Service;
    }

    @Transactional(readOnly = true)
    public ProgressOverviewResponse overview() {
        Long userId = currentUserProvider.userId();
        LocalDateTime now = LocalDateTime.now();
        return new ProgressOverviewResponse(
            wordRepository.countByUserId(userId),
            progressRepository.countDue(userId, now),
            progressRepository.countMastered(userId, 4),
            progressRepository.sumReviewCount(userId)
        );
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> due(int limit) {
        Long userId = currentUserProvider.userId();
        return progressRepository.findDue(
                userId,
                LocalDateTime.now(),
                PageRequest.of(0, Math.max(1, Math.min(limit, 100)))
            )
            .stream()
            .map(mapperService::toProgressResponse)
            .toList();
    }

    @Transactional
    public ProgressResponse reset(Long wordId) {
        Long userId = currentUserProvider.userId();
        Word word = wordRepository.findByIdAndUserId(wordId, userId)
            .orElseThrow(() -> new BusinessException("单词不存在或无权限访问"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        WordProgress progress = progressRepository.findByUserIdAndWordId(userId, wordId)
            .orElseGet(() -> {
                WordProgress created = new WordProgress();
                created.setUser(user);
                created.setWord(word);
                return progressRepository.save(created);
            });
        sm2Service.reset(progress);
        return mapperService.toProgressResponse(progress);
    }
}
