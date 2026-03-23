package com.bomin.aireviewreply.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String storeId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private String author;

    private Integer rating;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    @Column(length = 500)
    private String keywords;

    @Column(length = 2000)
    private String aiReply;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReplyStatus replyStatus = ReplyStatus.PENDING;

    @Column(nullable = false)
    private String platform; // NAVER, KAKAO, GOOGLE

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime analyzedAt;
    private LocalDateTime repliedAt;

    public enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    public enum ReplyStatus {
        PENDING,      // 분석 대기
        ANALYZED,     // 분석 완료, 답글 미생성
        GENERATED,    // AI 답글 생성됨 (승인 대기)
        APPROVED,     // 사장님 승인 완료
        SENT,         // 답글 발송 완료
        REJECTED      // 사장님 거절
    }
}
