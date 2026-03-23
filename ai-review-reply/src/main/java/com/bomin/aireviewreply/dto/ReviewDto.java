package com.bomin.aireviewreply.dto;

import com.bomin.aireviewreply.model.Review;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewDto {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "매장 ID는 필수입니다")
        private String storeId;
        @NotBlank(message = "리뷰 내용은 필수입니다")
        private String content;
        @NotBlank(message = "작성자는 필수입니다")
        private String author;
        private Integer rating;
        @NotBlank(message = "플랫폼은 필수입니다")
        private String platform;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String storeName;
        private String content;
        private String author;
        private Integer rating;
        private String sentiment;
        private List<String> keywords;
        private String aiReply;
        private String replyStatus;
        private String platform;
        private LocalDateTime createdAt;
        private LocalDateTime analyzedAt;

        public static Response from(Review review) {
            return Response.builder()
                    .id(review.getId())
                    .storeName(review.getStoreName())
                    .content(review.getContent())
                    .author(review.getAuthor())
                    .rating(review.getRating())
                    .sentiment(review.getSentiment() != null ? review.getSentiment().name() : null)
                    .keywords(review.getKeywords() != null ?
                            List.of(review.getKeywords().split(",")) : List.of())
                    .aiReply(review.getAiReply())
                    .replyStatus(review.getReplyStatus().name())
                    .platform(review.getPlatform())
                    .createdAt(review.getCreatedAt())
                    .analyzedAt(review.getAnalyzedAt())
                    .build();
        }
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class AnalysisResult {
        private String sentiment;
        private double confidenceScore;
        private List<String> keywords;
        private String summary;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class DashboardStats {
        private long totalReviews;
        private long positiveCount;
        private long negativeCount;
        private long neutralCount;
        private long pendingReplies;
        private long sentReplies;
        private double averageRating;
        private List<DailyTrend> weeklyTrend;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class DailyTrend {
        private String date;
        private long positive;
        private long negative;
        private long neutral;
    }
}
