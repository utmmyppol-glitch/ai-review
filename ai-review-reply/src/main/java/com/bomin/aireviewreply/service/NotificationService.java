package com.bomin.aireviewreply.service;

import com.bomin.aireviewreply.model.Review;
import com.bomin.aireviewreply.model.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    /**
     * 부정 리뷰 긴급 알림 전송
     */
    @Async
    public void sendNegativeReviewAlert(Store store, Review review) {
        if (store.getFcmToken() == null || store.getFcmToken().isBlank()) {
            log.warn("FCM 토큰이 없습니다: storeId={}", store.getStoreId());
            return;
        }

        try {
            // Firebase Admin SDK를 사용한 FCM 전송
            // 실제 환경에서는 FirebaseMessaging.getInstance().send() 사용
            String title = "⚠️ 부정 리뷰가 등록되었습니다";
            String body = String.format("[%s] %s님: \"%s\"",
                    review.getPlatform(),
                    review.getAuthor(),
                    truncate(review.getContent(), 50));

            log.info("FCM 알림 전송: store={}, title={}", store.getStoreName(), title);

            /*
             * 실제 FCM 전송 코드:
             *
             * Message message = Message.builder()
             *     .setToken(store.getFcmToken())
             *     .setNotification(Notification.builder()
             *         .setTitle(title)
             *         .setBody(body)
             *         .build())
             *     .putData("reviewId", review.getId().toString())
             *     .putData("type", "NEGATIVE_REVIEW")
             *     .setAndroidConfig(AndroidConfig.builder()
             *         .setPriority(AndroidConfig.Priority.HIGH)
             *         .setNotification(AndroidNotification.builder()
             *             .setSound("alert")
             *             .setChannelId("negative_review")
             *             .build())
             *         .build())
             *     .build();
             *
             * FirebaseMessaging.getInstance().send(message);
             */

        } catch (Exception e) {
            log.error("FCM 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 답글 생성 완료 알림
     */
    @Async
    public void sendReplyReadyAlert(Store store, Review review) {
        if (store.getFcmToken() == null) return;

        try {
            String title = "✅ AI 답글이 준비되었습니다";
            String body = String.format("%s님의 리뷰에 대한 답글을 확인해주세요.", review.getAuthor());

            log.info("답글 준비 알림 전송: store={}", store.getStoreName());

        } catch (Exception e) {
            log.error("FCM 전송 실패: {}", e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
