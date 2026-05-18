package com.englishwords.controller;

import com.englishwords.common.ApiResponse;
import com.englishwords.dto.PageResponse;
import com.englishwords.dto.dictionary.DictionaryEntryResponse;
import com.englishwords.service.DictionaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DictionaryEntryResponse>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(dictionaryService.search(keyword, page, size));
    }
}
