package com.bomin.aireviewreply.controller;

import com.bomin.aireviewreply.dto.ReviewDto;
import com.bomin.aireviewreply.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Review API", description = "리뷰 관리 및 AI 분석 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @Operation(summary = "리뷰 접수", description = "새 리뷰를 접수하고 AI 분석을 시작합니다")
    public ResponseEntity<ReviewDto.Response> createReview(
            @Valid @RequestBody ReviewDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request));
    }

    @GetMapping("/reviews")
    @Operation(summary = "리뷰 목록 조회")
    public ResponseEntity<List<ReviewDto.Response>> getReviews(
            @RequestParam String storeId,
            @RequestParam(required = false) String sentiment) {
        return ResponseEntity.ok(reviewService.getReviews(storeId, sentiment));
    }

    @GetMapping("/reviews/{id}/analysis")
    @Operation(summary = "감정 분석 결과 조회")
    public ResponseEntity<ReviewDto.AnalysisResult> getAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getAnalysis(id));
    }

    @PostMapping("/reviews/{id}/reply")
    @Operation(summary = "AI 답글 재생성")
    public ResponseEntity<ReviewDto.Response> regenerateReply(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.regenerateReply(id));
    }

    @PutMapping("/reviews/{id}/reply/approve")
    @Operation(summary = "답글 승인 및 발송")
    public ResponseEntity<ReviewDto.Response> approveReply(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReply(id));
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "대시보드 통계")
    public ResponseEntity<ReviewDto.DashboardStats> getDashboardStats(
            @RequestParam String storeId) {
        return ResponseEntity.ok(reviewService.getDashboardStats(storeId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "ai-review-reply"));
    }
}
