package com.bomin.aireviewreply.service;

import com.bomin.aireviewreply.dto.ReviewDto;
import com.bomin.aireviewreply.model.Review;
import com.bomin.aireviewreply.model.Store;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    @PersistenceContext
    private EntityManager em;

    private final AiAnalysisService aiAnalysisService;
    private final NotificationService notificationService;

    /**
     * 리뷰 접수 + 자동 분석
     */
    public ReviewDto.Response createReview(ReviewDto.CreateRequest request) {
        // 매장 조회
        TypedQuery<Store> storeQuery = em.createQuery(
                "SELECT s FROM Store s WHERE s.storeId = :storeId", Store.class);
        storeQuery.setParameter("storeId", request.getStoreId());
        Store store = storeQuery.getResultList().stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다: " + request.getStoreId()));

        // 리뷰 저장
        Review review = Review.builder()
                .storeId(request.getStoreId())
                .storeName(store.getStoreName())
                .content(request.getContent())
                .author(request.getAuthor())
                .rating(request.getRating())
                .platform(request.getPlatform())
                .replyStatus(Review.ReplyStatus.PENDING)
                .build();

        em.persist(review);
        em.flush();

        // 비동기 AI 분석 실행
        processReviewAsync(review.getId(), store);

        return ReviewDto.Response.from(review);
    }

    /**
     * 비동기 리뷰 분석 + 답글 생성
     */
    @Async
    public void processReviewAsync(Long reviewId, Store store) {
        try {
            Review review = em.find(Review.class, reviewId);
            if (review == null) return;

            // 1. 감정 분석
            ReviewDto.AnalysisResult analysis = aiAnalysisService.analyzeReview(review.getContent());

            review.setSentiment(Review.Sentiment.valueOf(analysis.getSentiment()));
            review.setKeywords(String.join(",", analysis.getKeywords()));
            review.setAnalyzedAt(LocalDateTime.now());
            review.setReplyStatus(Review.ReplyStatus.ANALYZED);

            // 2. 부정 리뷰 → 긴급 FCM 알림
            if (review.getSentiment() == Review.Sentiment.NEGATIVE) {
                notificationService.sendNegativeReviewAlert(store, review);
            }

            // 3. AI 답글 생성
            String reply = aiAnalysisService.generateReply(
                    review.getContent(),
                    analysis.getSentiment(),
                    String.join(", ", analysis.getKeywords()),
                    store.getReplyTone()
            );

            if (reply != null) {
                review.setAiReply(reply);
                review.setReplyStatus(Review.ReplyStatus.GENERATED);

                // 자동 답글 모드면 바로 발송
                if (store.isAutoReply() && review.getSentiment() != Review.Sentiment.NEGATIVE) {
                    review.setReplyStatus(Review.ReplyStatus.SENT);
                    review.setRepliedAt(LocalDateTime.now());
                    log.info("자동 답글 발송: reviewId={}", reviewId);
                } else {
                    notificationService.sendReplyReadyAlert(store, review);
                }
            }

            em.merge(review);

        } catch (Exception e) {
            log.error("리뷰 처리 실패: reviewId={}, error={}", reviewId, e.getMessage());
        }
    }

    /**
     * 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewDto.Response> getReviews(String storeId, String sentiment) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Review r WHERE r.storeId = :storeId");
        if (sentiment != null && !sentiment.isBlank()) {
            jpql.append(" AND r.sentiment = :sentiment");
        }
        jpql.append(" ORDER BY r.createdAt DESC");

        TypedQuery<Review> query = em.createQuery(jpql.toString(), Review.class);
        query.setParameter("storeId", storeId);
        if (sentiment != null && !sentiment.isBlank()) {
            query.setParameter("sentiment", Review.Sentiment.valueOf(sentiment));
        }

        return query.getResultList().stream()
                .map(ReviewDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 감정 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public ReviewDto.AnalysisResult getAnalysis(Long reviewId) {
        Review review = em.find(Review.class, reviewId);
        if (review == null) throw new EntityNotFoundException("리뷰를 찾을 수 없습니다");

        return ReviewDto.AnalysisResult.builder()
                .sentiment(review.getSentiment() != null ? review.getSentiment().name() : null)
                .keywords(review.getKeywords() != null ?
                        List.of(review.getKeywords().split(",")) : List.of())
                .build();
    }

    /**
     * AI 답글 재생성
     */
    public ReviewDto.Response regenerateReply(Long reviewId) {
        Review review = em.find(Review.class, reviewId);
        if (review == null) throw new EntityNotFoundException("리뷰를 찾을 수 없습니다");

        TypedQuery<Store> storeQuery = em.createQuery(
                "SELECT s FROM Store s WHERE s.storeId = :storeId", Store.class);
        storeQuery.setParameter("storeId", review.getStoreId());
        Store store = storeQuery.getSingleResult();

        String reply = aiAnalysisService.generateReply(
                review.getContent(),
                review.getSentiment() != null ? review.getSentiment().name() : "NEUTRAL",
                review.getKeywords(),
                store.getReplyTone()
        );

        review.setAiReply(reply);
        review.setReplyStatus(Review.ReplyStatus.GENERATED);
        em.merge(review);

        return ReviewDto.Response.from(review);
    }

    /**
     * 답글 승인
     */
    public ReviewDto.Response approveReply(Long reviewId) {
        Review review = em.find(Review.class, reviewId);
        if (review == null) throw new EntityNotFoundException("리뷰를 찾을 수 없습니다");

        review.setReplyStatus(Review.ReplyStatus.APPROVED);
        review.setRepliedAt(LocalDateTime.now());
        em.merge(review);

        // TODO: 실제 플랫폼 API로 답글 발송
        review.setReplyStatus(Review.ReplyStatus.SENT);
        em.merge(review);

        return ReviewDto.Response.from(review);
    }

    /**
     * 대시보드 통계
     */
    @Transactional(readOnly = true)
    public ReviewDto.DashboardStats getDashboardStats(String storeId) {
        // 전체 통계
        TypedQuery<Long> totalQuery = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.storeId = :storeId", Long.class);
        totalQuery.setParameter("storeId", storeId);
        long total = totalQuery.getSingleResult();

        // 감정별 카운트
        long positive = countBySentiment(storeId, Review.Sentiment.POSITIVE);
        long negative = countBySentiment(storeId, Review.Sentiment.NEGATIVE);
        long neutral = countBySentiment(storeId, Review.Sentiment.NEUTRAL);

        // 답글 상태
        TypedQuery<Long> pendingQuery = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.storeId = :storeId AND r.replyStatus IN (:statuses)", Long.class);
        pendingQuery.setParameter("storeId", storeId);
        pendingQuery.setParameter("statuses", List.of(Review.ReplyStatus.PENDING, Review.ReplyStatus.ANALYZED, Review.ReplyStatus.GENERATED));
        long pending = pendingQuery.getSingleResult();

        TypedQuery<Long> sentQuery = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.storeId = :storeId AND r.replyStatus = :status", Long.class);
        sentQuery.setParameter("storeId", storeId);
        sentQuery.setParameter("status", Review.ReplyStatus.SENT);
        long sent = sentQuery.getSingleResult();

        // 평균 별점
        TypedQuery<Double> avgQuery = em.createQuery(
                "SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.storeId = :storeId AND r.rating IS NOT NULL", Double.class);
        avgQuery.setParameter("storeId", storeId);
        double avgRating = avgQuery.getSingleResult();

        return ReviewDto.DashboardStats.builder()
                .totalReviews(total)
                .positiveCount(positive)
                .negativeCount(negative)
                .neutralCount(neutral)
                .pendingReplies(pending)
                .sentReplies(sent)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .weeklyTrend(List.of()) // 간소화
                .build();
    }

    private long countBySentiment(String storeId, Review.Sentiment sentiment) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.storeId = :storeId AND r.sentiment = :sentiment", Long.class);
        query.setParameter("storeId", storeId);
        query.setParameter("sentiment", sentiment);
        return query.getSingleResult();
    }
}
