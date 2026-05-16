package com.englishwords.service;

import com.englishwords.common.BusinessException;
import com.englishwords.dto.word.WordBookRequest;
import com.englishwords.dto.word.WordBookResponse;
import com.englishwords.entity.User;
import com.englishwords.entity.WordBook;
import com.englishwords.repository.UserRepository;
import com.englishwords.repository.WordBookRepository;
import com.englishwords.security.CurrentUserProvider;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordBookService {

    private final WordBookRepository wordBookRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MapperService mapperService;

    public WordBookService(
        WordBookRepository wordBookRepository,
        UserRepository userRepository,
        CurrentUserProvider currentUserProvider,
        MapperService mapperService
    ) {
        this.wordBookRepository = wordBookRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.mapperService = mapperService;
    }

    @Transactional(readOnly = true)
    public List<WordBookResponse> list() {
        Long userId = currentUserProvider.userId();
        return wordBookRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            .stream()
            .map(mapperService::toWordBookResponse)
            .toList();
    }

    @Transactional
    public WordBookResponse create(WordBookRequest request) {
        Long userId = currentUserProvider.userId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        WordBook wordBook = new WordBook();
        wordBook.setUser(user);
        wordBook.setName(request.name().trim());
        wordBook.setDescription(trimToNull(request.description()));
        wordBookRepository.save(wordBook);
        return mapperService.toWordBookResponse(wordBook);
    }

    @Transactional
    public WordBookResponse update(Long id, WordBookRequest request) {
        WordBook wordBook = getOwnedBook(id);
        wordBook.setName(request.name().trim());
        wordBook.setDescription(trimToNull(request.description()));
        return mapperService.toWordBookResponse(wordBook);
    }

    @Transactional
    public void delete(Long id) {
        WordBook wordBook = getOwnedBook(id);
        wordBookRepository.delete(wordBook);
    }

    public WordBook getOwnedBook(Long id) {
        Long userId = currentUserProvider.userId();
        return wordBookRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BusinessException("词库不存在或无权限访问"));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
