package com.englishwords.service;

import com.englishwords.common.BusinessException;
import com.englishwords.dto.PageResponse;
import com.englishwords.dto.word.WordImportResponse;
import com.englishwords.dto.word.WordRequest;
import com.englishwords.dto.word.WordResponse;
import com.englishwords.entity.User;
import com.englishwords.entity.Word;
import com.englishwords.entity.WordBook;
import com.englishwords.entity.WordProgress;
import com.englishwords.repository.UserRepository;
import com.englishwords.repository.WordProgressRepository;
import com.englishwords.repository.WordRepository;
import com.englishwords.security.CurrentUserProvider;
import com.englishwords.service.WordImportParser.ImportWordRow;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WordService {

    private final WordRepository wordRepository;
    private final WordProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final WordBookService wordBookService;
    private final CurrentUserProvider currentUserProvider;
    private final MapperService mapperService;
    private final WordImportParser importParser;

    public WordService(
        WordRepository wordRepository,
        WordProgressRepository progressRepository,
        UserRepository userRepository,
        WordBookService wordBookService,
        CurrentUserProvider currentUserProvider,
        MapperService mapperService,
        WordImportParser importParser
    ) {
        this.wordRepository = wordRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.wordBookService = wordBookService;
        this.currentUserProvider = currentUserProvider;
        this.mapperService = mapperService;
        this.importParser = importParser;
    }

    @Transactional(readOnly = true)
    public PageResponse<WordResponse> list(
        Long bookId,
        String keyword,
        String tag,
        Integer masteryLevel,
        Boolean onlyDue,
        int page,
        int size
    ) {
        Long userId = currentUserProvider.userId();
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<Word> result = wordRepository.findAll(
            specification(userId, bookId, keyword, tag, masteryLevel, Boolean.TRUE.equals(onlyDue)),
            pageRequest
        );
        return new PageResponse<>(
            result.getContent().stream().map(mapperService::toWordResponse).toList(),
            result.getTotalElements(),
            result.getNumber(),
            result.getSize(),
            result.getTotalPages()
        );
    }

    @Transactional
    public WordResponse create(WordRequest request) {
        Long userId = currentUserProvider.userId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        WordBook book = wordBookService.getOwnedBook(request.bookId());
        String term = normalizeTerm(request.word());

        wordRepository.findByUserIdAndWordBookIdAndTermIgnoreCase(userId, book.getId(), term)
            .ifPresent(word -> {
                throw new BusinessException("该词库中已存在该单词");
            });

        Word word = new Word();
        word.setUser(user);
        word.setWordBook(book);
        applyWordFields(word, request);
        word.setTerm(term);
        wordRepository.save(word);
        createProgress(user, word);
        return mapperService.toWordResponse(word);
    }

    @Transactional
    public WordResponse update(Long id, WordRequest request) {
        Long userId = currentUserProvider.userId();
        Word word = getOwnedWord(id);
        WordBook book = wordBookService.getOwnedBook(request.bookId());
        String term = normalizeTerm(request.word());

        wordRepository.findByUserIdAndWordBookIdAndTermIgnoreCase(userId, book.getId(), term)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new BusinessException("该词库中已存在该单词");
            });

        word.setWordBook(book);
        applyWordFields(word, request);
        word.setTerm(term);
        return mapperService.toWordResponse(word);
    }

    @Transactional
    public void delete(Long id) {
        Word word = getOwnedWord(id);
        wordRepository.delete(word);
    }

    @Transactional
    public WordImportResponse importWords(Long bookId, MultipartFile file) {
        Long userId = currentUserProvider.userId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        WordBook book = wordBookService.getOwnedBook(bookId);
        List<ImportWordRow> rows = importParser.parse(file);

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (ImportWordRow row : rows) {
            if (isBlank(row.word()) || isBlank(row.translation())) {
                skipped++;
                errors.add("第 " + row.rowNumber() + " 行缺少 word 或 translation");
                continue;
            }
            String term = normalizeTerm(row.word());
            Word word = wordRepository.findByUserIdAndWordBookIdAndTermIgnoreCase(userId, bookId, term)
                .orElse(null);
            if (word == null) {
                word = new Word();
                word.setUser(user);
                word.setWordBook(book);
                word.setTerm(term);
                word.setTranslation(row.translation().trim());
                word.setPhonetic(trimToNull(row.phonetic()));
                word.setExample(trimToNull(row.example()));
                word.setTags(trimToNull(row.tags()));
                wordRepository.save(word);
                createProgress(user, word);
                created++;
            } else {
                word.setTranslation(row.translation().trim());
                word.setPhonetic(trimToNull(row.phonetic()));
                word.setExample(trimToNull(row.example()));
                word.setTags(trimToNull(row.tags()));
                updated++;
            }
        }

        return new WordImportResponse(rows.size(), created, updated, skipped, errors);
    }

    public Word getOwnedWord(Long wordId) {
        Long userId = currentUserProvider.userId();
        return wordRepository.findByIdAndUserId(wordId, userId)
            .orElseThrow(() -> new BusinessException("单词不存在或无权限访问"));
    }

    public WordProgress ensureProgress(User user, Word word) {
        return progressRepository.findByUserIdAndWordId(user.getId(), word.getId())
            .orElseGet(() -> createProgress(user, word));
    }

    private WordProgress createProgress(User user, Word word) {
        WordProgress progress = new WordProgress();
        progress.setUser(user);
        progress.setWord(word);
        progress.setEaseFactor(2.5);
        progress.setMasteryLevel(0);
        progressRepository.save(progress);
        word.setProgress(progress);
        return progress;
    }

    private void applyWordFields(Word word, WordRequest request) {
        word.setTranslation(request.translation().trim());
        word.setPhonetic(trimToNull(request.phonetic()));
        word.setExample(trimToNull(request.example()));
        word.setTags(trimToNull(request.tags()));
    }

    private Specification<Word> specification(
        Long userId,
        Long bookId,
        String keyword,
        String tag,
        Integer masteryLevel,
        boolean onlyDue
    ) {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("wordBook", JoinType.LEFT);
                root.fetch("progress", JoinType.LEFT);
            }
            Join<Word, WordProgress> progressJoin = root.join("progress", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (bookId != null) {
                predicates.add(cb.equal(root.get("wordBook").get("id"), bookId));
            }
            if (!isBlank(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("term")), like),
                    cb.like(cb.lower(root.get("translation")), like)
                ));
            }
            if (!isBlank(tag)) {
                predicates.add(cb.like(cb.lower(root.get("tags")), "%" + tag.trim().toLowerCase() + "%"));
            }
            if (masteryLevel != null) {
                predicates.add(cb.equal(progressJoin.get("masteryLevel"), masteryLevel));
            }
            if (onlyDue) {
                predicates.add(cb.greaterThan(progressJoin.get("reviewCount"), 0));
            }
            query.orderBy(cb.desc(root.get("updatedAt")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeTerm(String value) {
        return value.trim();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
