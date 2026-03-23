package com.bomin.aireviewreply.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stores")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String storeId;

    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String ownerEmail;

    private String fcmToken;

    @Column(length = 1000)
    private String replyTone; // 답글 톤 설정 (친근, 정중, 격식)

    @Builder.Default
    private boolean autoReply = false; // 자동 답글 ON/OFF

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
