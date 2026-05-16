package com.englishwords.controller;

import com.englishwords.common.ApiResponse;
import com.englishwords.dto.quiz.QuizQuestionResponse;
import com.englishwords.dto.quiz.QuizRecordResponse;
import com.englishwords.dto.quiz.QuizSubmitRequest;
import com.englishwords.dto.quiz.QuizSubmitResponse;
import com.englishwords.enums.QuizMode;
import com.englishwords.service.QuizService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/random")
    public ApiResponse<List<QuizQuestionResponse>> random(
        @RequestParam(required = false) Long bookId,
        @RequestParam(defaultValue = "10") int count,
        @RequestParam(defaultValue = "MIXED") QuizMode mode,
        @RequestParam(defaultValue = "false") boolean onlyDue
    ) {
        return ApiResponse.ok(quizService.random(bookId, count, mode, onlyDue));
    }

    @PostMapping("/submit")
    public ApiResponse<QuizSubmitResponse> submit(@Valid @RequestBody QuizSubmitRequest request) {
        return ApiResponse.ok(quizService.submit(request));
    }

    @GetMapping("/history")
    public ApiResponse<List<QuizRecordResponse>> history(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(quizService.history(limit));
    }
}
