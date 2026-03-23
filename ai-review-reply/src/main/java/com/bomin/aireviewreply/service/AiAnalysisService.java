package com.bomin.aireviewreply.service;

import com.bomin.aireviewreply.dto.ReviewDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final RestTemplate openAiRestTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String CACHE_PREFIX = "review:analysis:";

    /**
     * 리뷰 감정 분석
     */
    public ReviewDto.AnalysisResult analyzeReview(String reviewContent) {
        // Redis 캐시 확인
        String cacheKey = CACHE_PREFIX + reviewContent.hashCode();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Cache hit for review analysis");
            return objectMapper.convertValue(cached, ReviewDto.AnalysisResult.class);
        }

        String prompt = """
                다음 리뷰를 분석해주세요. 반드시 아래 JSON 형식으로만 응답하세요.
                
                리뷰: "%s"
                
                응답 형식:
                {
                    "sentiment": "POSITIVE" 또는 "NEGATIVE" 또는 "NEUTRAL",
                    "confidenceScore": 0.0~1.0 사이의 신뢰도,
                    "keywords": ["키워드1", "키워드2", "키워드3"],
                    "summary": "리뷰 요약 (한 줄)"
                }
                """.formatted(reviewContent);

        try {
            Map<String, Object> request = buildChatRequest(prompt);
            String response = openAiRestTemplate.postForObject(OPENAI_API_URL, request, String.class);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // JSON 파싱
            String jsonStr = extractJson(content);
            JsonNode analysisNode = objectMapper.readTree(jsonStr);

            ReviewDto.AnalysisResult result = ReviewDto.AnalysisResult.builder()
                    .sentiment(analysisNode.path("sentiment").asText("NEUTRAL"))
                    .confidenceScore(analysisNode.path("confidenceScore").asDouble(0.5))
                    .keywords(parseKeywords(analysisNode.path("keywords")))
                    .summary(analysisNode.path("summary").asText(""))
                    .build();

            // Redis 캐시 저장 (1시간)
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofHours(1));

            return result;
        } catch (Exception e) {
            log.error("AI 분석 실패: {}", e.getMessage());
            return ReviewDto.AnalysisResult.builder()
                    .sentiment("NEUTRAL")
                    .confidenceScore(0.0)
                    .keywords(List.of())
                    .summary("분석 실패")
                    .build();
        }
    }

    /**
     * AI 답글 생성
     */
    public String generateReply(String reviewContent, String sentiment, String keywords, String tone) {
        String toneGuide = switch (tone != null ? tone : "friendly") {
            case "formal" -> "격식체로 정중하게";
            case "casual" -> "반말로 친근하게";
            default -> "존댓말로 친근하고 따뜻하게";
        };

        String prompt = """
                당신은 소상공인 사장님의 리뷰 답글 작성을 돕는 AI입니다.
                다음 리뷰에 대한 답글을 작성해주세요.
                
                [리뷰 정보]
                - 내용: "%s"
                - 감정: %s
                - 키워드: %s
                
                [작성 규칙]
                - 톤: %s
                - 2~4문장으로 간결하게
                - 감사 인사 포함
                - 부정 리뷰: 사과 + 개선 약속 (변명 X)
                - 긍정 리뷰: 감사 + 재방문 유도
                - 자연스러운 한국어로 작성
                - 답글만 작성 (다른 설명 X)
                """.formatted(reviewContent, sentiment, keywords, toneGuide);

        try {
            Map<String, Object> request = buildChatRequest(prompt);
            String response = openAiRestTemplate.postForObject(OPENAI_API_URL, request, String.class);
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            log.error("답글 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildChatRequest(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        request.put("temperature", 0.7);
        request.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 한국어 리뷰 분석 및 답글 작성 전문 AI입니다."),
                Map.of("role", "user", "content", prompt)
        ));
        return request;
    }

    private String extractJson(String content) {
        // ```json ... ``` 블록 추출
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.indexOf("```", start);
            return content.substring(start, end).trim();
        }
        if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.indexOf("```", start);
            return content.substring(start, end).trim();
        }
        return content.trim();
    }

    private List<String> parseKeywords(JsonNode keywordsNode) {
        List<String> keywords = new ArrayList<>();
        if (keywordsNode.isArray()) {
            keywordsNode.forEach(node -> keywords.add(node.asText()));
        }
        return keywords;
    }
}
