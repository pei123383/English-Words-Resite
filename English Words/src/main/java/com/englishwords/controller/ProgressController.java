package com.englishwords.controller;

import com.englishwords.common.ApiResponse;
import com.englishwords.dto.progress.ProgressOverviewResponse;
import com.englishwords.dto.progress.ProgressResponse;
import com.englishwords.service.ProgressService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/overview")
    public ApiResponse<ProgressOverviewResponse> overview() {
        return ApiResponse.ok(progressService.overview());
    }

    @GetMapping("/due")
    public ApiResponse<List<ProgressResponse>> due(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(progressService.due(limit));
    }

    @PutMapping("/{wordId}/reset")
    public ApiResponse<ProgressResponse> reset(@PathVariable Long wordId) {
        return ApiResponse.ok(progressService.reset(wordId));
    }
}
