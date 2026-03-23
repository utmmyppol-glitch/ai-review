# 🤖 AI Review Reply — 소상공인 AI 자동 리뷰 답글 시스템

> 리뷰가 달리면 AI가 감정을 분석하고, 맞춤 답글을 자동 생성합니다.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=flat-square&logo=openai&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![FCM](https://img.shields.io/badge/Firebase_FCM-FFCA28?style=flat-square&logo=firebase&logoColor=black)

---

## 📌 프로젝트 소개

소상공인 사장님들이 네이버/카카오 리뷰에 일일이 답글을 다는 것은 시간과 노력이 많이 드는 작업입니다.
**AI Review Reply**는 리뷰가 접수되면 자동으로 감정 분석(긍정/부정/중립)을 수행하고, 맞춤형 답글을 생성하여 사장님의 업무를 자동화합니다.

### 핵심 기능

- **리뷰 감정 분석**: OpenAI API를 활용하여 리뷰의 감정(긍정/부정/중립)과 키워드를 자동 추출
- **AI 맞춤 답글 생성**: 감정과 키워드에 맞는 자연스러운 답글을 자동 생성
- **긴급 알림 시스템**: 부정 리뷰 감지 시 FCM 푸시 알림으로 사장님에게 즉시 통보
- **답글 승인 플로우**: 자동 발송 / 사장님 승인 후 발송 모드 선택 가능
- **리뷰 대시보드**: 감정 분석 통계, 답글 현황, 트렌드 차트 제공

---

## 🏗 아키텍처

```
[리뷰 접수 (Webhook/API)] 
    ↓
[Spring Boot 서버]
    ├── 감정 분석 (OpenAI API)
    ├── 키워드 추출
    ├── 답글 생성 (OpenAI API)
    ↓
[Redis 캐시] ← 답글 템플릿 & 분석 결과 캐싱
    ↓
[FCM 푸시 알림] → 부정 리뷰 긴급 알림
    ↓
[사장님 승인] → 답글 발송
```

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3, Spring WebFlux |
| AI | OpenAI GPT API (감정 분석 + 답글 생성) |
| Cache | Redis (분석 결과 캐싱, 중복 리뷰 방지) |
| Push | Firebase Cloud Messaging (FCM) |
| DB | MySQL 8.0 (리뷰 이력, 답글 이력) |
| Auth | JWT + Spring Security |
| Docs | Swagger (SpringDoc OpenAPI) |

---

## 📁 프로젝트 구조

```
src/main/java/com/bomin/aireviewreply/
├── config/          # OpenAI, Redis, FCM, Security 설정
├── controller/      # REST API 엔드포인트
├── dto/             # Request/Response DTO
├── model/           # Entity 클래스
├── service/         # 비즈니스 로직 (AI 분석, 답글 생성, 알림)
└── AiReviewReplyApplication.java
```

---

## 🚀 실행 방법

```bash
# 1. 환경변수 설정
export OPENAI_API_KEY=your_openai_api_key
export FCM_SERVER_KEY=your_fcm_server_key

# 2. Redis 실행
docker run -d -p 6379:6379 redis:alpine

# 3. MySQL 실행
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ai_review mysql:8.0

# 4. 애플리케이션 실행
./gradlew bootRun
```

---

## 📡 API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/reviews` | 리뷰 접수 (Webhook) |
| GET | `/api/reviews` | 리뷰 목록 조회 |
| GET | `/api/reviews/{id}/analysis` | 감정 분석 결과 조회 |
| POST | `/api/reviews/{id}/reply` | AI 답글 생성 |
| PUT | `/api/reviews/{id}/reply/approve` | 답글 승인 |
| GET | `/api/dashboard/stats` | 대시보드 통계 |

---

## 🔧 트러블슈팅 가이드

**OpenAI API Rate Limit 초과**
- Redis 캐싱으로 동일 패턴 리뷰 재분석 방지
- 요청 큐잉 + 지수 백오프 재시도 로직

**FCM 토큰 만료**
- 토큰 갱신 실패 시 DB 플래그 업데이트 + 재등록 유도

---

## 📄 라이선스

MIT License

---

## 👨‍💻 만든 사람

**김보민** — [@utmmyppol-glitch](https://github.com/utmmyppol-glitch)
