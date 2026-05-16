package com.englishwords.controller;

import com.englishwords.common.ApiResponse;
import com.englishwords.dto.word.WordBookRequest;
import com.englishwords.dto.word.WordBookResponse;
import com.englishwords.service.WordBookService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/word-books")
public class WordBookController {

    private final WordBookService wordBookService;

    public WordBookController(WordBookService wordBookService) {
        this.wordBookService = wordBookService;
    }

    @GetMapping
    public ApiResponse<List<WordBookResponse>> list() {
        return ApiResponse.ok(wordBookService.list());
    }

    @PostMapping
    public ApiResponse<WordBookResponse> create(@Valid @RequestBody WordBookRequest request) {
        return ApiResponse.ok(wordBookService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WordBookResponse> update(@PathVariable Long id, @Valid @RequestBody WordBookRequest request) {
        return ApiResponse.ok(wordBookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        wordBookService.delete(id);
        return ApiResponse.ok();
    }
}
