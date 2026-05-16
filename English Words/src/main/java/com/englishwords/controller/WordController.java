package com.englishwords.controller;

import com.englishwords.common.ApiResponse;
import com.englishwords.dto.PageResponse;
import com.englishwords.dto.word.WordImportResponse;
import com.englishwords.dto.word.WordRequest;
import com.englishwords.dto.word.WordResponse;
import com.englishwords.service.WordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/words")
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WordResponse>> list(
        @RequestParam(required = false) Long bookId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) Integer masteryLevel,
        @RequestParam(required = false, defaultValue = "false") Boolean onlyDue,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(wordService.list(bookId, keyword, tag, masteryLevel, onlyDue, page, size));
    }

    @PostMapping
    public ApiResponse<WordResponse> create(@Valid @RequestBody WordRequest request) {
        return ApiResponse.ok(wordService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WordResponse> update(@PathVariable Long id, @Valid @RequestBody WordRequest request) {
        return ApiResponse.ok(wordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        wordService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/import")
    public ApiResponse<WordImportResponse> importWords(
        @RequestParam Long bookId,
        @RequestParam MultipartFile file
    ) {
        return ApiResponse.ok(wordService.importWords(bookId, file));
    }
}
