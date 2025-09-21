# 송금 서비스 백엔드 (Money Transfer Service Backend)

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-brightgreen) ![MySQL](https://img.shields.io/badge/MySQL-8.0-blue) ![Docker](https://img.shields.io/badge/Docker-Compose-blue)

## 목차

- [개요](#개요)
  - [핵심 특징](#핵심-특징)
- [아키텍처](#아키텍처)
  - [멀티모듈 Clean Architecture](#멀티모듈-clean-architecture)
  - [의존성 규칙 (Clean Architecture)](#의존성-규칙-clean-architecture)
- [기술 스택](#기술-스택)
  - [Core Technologies](#core-technologies)
  - [Build & Deployment](#build--deployment)
  - [Testing & Quality](#testing--quality)
- [데이터베이스 설계](#데이터베이스-설계)
  - [데이터베이스 스키마](#데이터베이스-스키마)
  - [주요 제약사항 및 특징](#주요-제약사항-및-특징)
- [API 명세](#api-명세)
  - [주요 엔드포인트](#주요-엔드포인트)
  - [API 요청/응답 예시](#api-요청응답-예시)
  - [API 문서화](#api-문서화)
- [비즈니스 규칙](#비즈니스-규칙)
  - [거래 한도](#거래-한도)
  - [수수료 정책](#수수료-정책)
  - [검증 규칙](#검증-규칙)
- [동시성 제어](#동시성-제어)
  - [다층 동시성 제어 전략](#다층-동시성-제어-전략)
- [테스트 전략](#테스트-전략)
  - [포괄적인 테스트 커버리지](#포괄적인-테스트-커버리지)
  - [테스트 실행](#테스트-실행)
- [빌드 및 실행](#빌드-및-실행)
  - [사전 요구사항](#사전-요구사항)
  - [Docker Compose 실행 (권장)](#2-docker-compose-실행-권장)
  - [로컬 개발 환경 실행](#3-로컬-개발-환경-실행)
  - [서비스 확인](#서비스-확인)

---

## 개요

- **Clean Architecture**와 **Domain-Driven Design** 패턴을 적용한 엔터프라이즈급 송금 서비스 백엔드 시스템
- 계좌 관리, 입출금, 이체, 거래내역 조회 등의 핵심 금융 서비스 기능을 제공하며, **동시성 제어**와 **확장성**을 중점적으로 고려하여 설계

### 핵심 특징

- **Clean Architecture** 기반 멀티모듈 설계
- **동시성 이슈** (낙관적/비관적 락)
- **일일 한도 관리** (출금: 100만원, 이체: 300만원)
- **이체 수수료 계산** (이체 금액의 1%)
- **포괄적인 테스트 커버리지** (단위/통합/동시성 테스트)
- **Docker Compose** 기반 원클릭 배포
- **Swagger/OpenAPI** 완전 문서화

## 아키텍처

### 멀티모듈 Clean Architecture

```
money-transfer-backend/
├── money-transfer-domain/       # Domain Layer (핵심 비즈니스 로직)
│   ├── account/                 # Account 도메인 (엔티티, 포트, 상태)
│   ├── user/                    # User 도메인 (엔티티, 포트)
│   ├── transaction/             # Transaction 도메인 (엔티티, 타입, 포트)
│   ├── dailylimit/              # DailyLimit 도메인 (엔티티, 포트)
│   └── common/                  # 공통 도메인 객체 (PageQuery, PageResult)
│
├── money-transfer-application/  # Application Layer (유스케이스)
│   ├── usecase/                 # 비즈니스 유스케이스 구현
│   │   ├── account/             # 계좌 관리 (등록/삭제/조회)
│   │   └── transaction/         # 거래 처리 (입금/출금/이체/조회)
│   └── dto/                     # 애플리케이션 DTO
│
├── money-transfer-persistence/  # Infrastructure Layer (데이터 계층)
│   ├── entity/                  # JPA 엔티티
│   ├── repository/              # JPA Repository 인터페이스
│   └── adapter/                 # Port 구현체 (Adapter)
│
├── money-transfer-api/          # Presentation Layer (API 계층)
│   ├── controller/              # REST 컨트롤러
│   ├── dto/                     # API Request/Response DTO
│   ├── mapper/                  # DTO ↔ Domain 변환
│   ├── config/                  # Spring 설정 클래스
│   └── exception/               # 전역 예외 처리
│
└── money-transfer-common/       # Shared Utilities
    ├── constant/                # 상수, 비즈니스 상수, 에러 메시지
    └── util/                    # 유틸리티 및 검증 로직
```

### 의존성 규칙 (Clean Architecture)
- 의존성 흐름: API → Application → Domain ← Persistence
- 핵심 원칙: Domain은 다른 레이어에 의존하지 않음 (Clean Architecture 규칙)
 
```
                    ┌─────────────────┐
                    │   Common        │
                    │   Module        │
                    │ (Utils, Const.) │
                    └─────────┬───────┘
                              │ (사용됨)
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Module    │───▶│ Application     │───▶│   Domain        │
│  (REST API)     │    │   Module        │    │   Module        │
│ • Controllers   │    │ • Use Cases     │    │ • Entities      │
│ • DTOs          │    │ • App Services  │    │ • Ports         │
│ • Exception     │    │ • App DTOs      │    │ • Domain Logic  │
│   Handlers      │    └─────────────────┘    └─────────────────┘
└─────────────────┘                                      ▲
                                                         │
                       ┌─────────────────┐               │
                       │  Persistence    │───────────────┘
                       │   Module        │  (Port Interface 구현)
                       │ • JPA Entities  │
                       │ • Repositories  │
                       │ • Port Adapters │
                       └─────────────────┘
```

## 기술 스택

### Core Technologies
- **Java 17** - 최신 JVM 기능 활용
- **Spring Boot 3.2.0** - 엔터프라이즈 프레임워크
- **Spring Data JPA (Hibernate)** - ORM 및 데이터 접근
- **MySQL 8.0** - 프로덕션 데이터베이스
- **H2 Database** - 개발/테스트용 인메모리 DB

### Build & Deployment
- **Gradle 8.12** - 멀티모듈 빌드 시스템
- **Docker & Docker Compose** - 컨테이너화 및 오케스트레이션
- **SpringDoc OpenAPI 3** - API 문서화

### Testing & Quality
- **JUnit 5** - 단위 테스트 프레임워크
- **Mockito** - 모킹 프레임워크
- **AssertJ** - 어설션 라이브러리
- **Spring Boot Test** - 통합 테스트 지원

## 데이터베이스 설계

### 데이터베이스 스키마

#### 주요 테이블

**USERS** (사용자)
- `id` (PK) - 사용자 ID
- `name` - 사용자명
- `email` (UK) - 이메일 (유니크)
- `id_card_no` - 주민번호
- `id_card_no_norm` (UK) - 정규화된 주민번호 (유니크)
- `created_at` - 생성시간

**ACCOUNTS** (계좌)
- `id` (PK) - 계좌 ID
- `user_id` (FK) - 사용자 ID 참조
- `bank_code` - 은행코드
- `account_no` - 계좌번호
- `account_no_norm` (UK) - 정규화된 계좌번호 (은행코드와 복합 유니크)
- `balance` - 잔액
- `status` - 계좌상태 (ACTIVE/DEACTIVATED)
- `version` - 낙관적 락 버전
- `created_at`, `updated_at`, `deactivated_at` - 시간 필드

**TRANSACTIONS** (거래내역)
- `id` (PK) - 거래 ID
- `account_id` (FK) - 계좌 ID 참조
- `related_account_id` (FK) - 연관 계좌 (이체시)
- `transaction_type` - 거래유형 (DEPOSIT/WITHDRAW/TRANSFER)
- `amount` - 거래금액
- `balance_after` - 거래후 잔액
- `fee` - 수수료
- `description` - 거래설명
- `created_at` - 거래시간

**DAILY_LIMITS** (일일한도)
- `id` (PK) - 한도 ID
- `account_id` (FK) - 계좌 ID 참조
- `limit_date` - 한도 적용 날짜
- `withdraw_used` - 일일 출금 사용량
- `transfer_used` - 일일 이체 사용량
- `created_at`, `updated_at` - 시간 필드

### 주요 제약사항 및 특징
- **Unique Constraints**: 이메일, 정규화된 주민번호, (은행코드 + 정규화된 계좌번호)
- **Optimistic Locking**: 계좌 엔티티의 동시 수정 방지
- **Indexes**: 조회 성능 최적화를 위한 복합 인덱스
- **Soft Delete**: 계좌 비활성화 (물리적 삭제 방지)

## API 명세

### 주요 엔드포인트

| 기능 | Method | Endpoint | 설명 |
|------|---------|----------|------|
| **계좌 등록** | `POST` | `/accounts` | 새 계좌 생성 (사용자 정보 포함) |
| **계좌 삭제** | `DELETE` | `/accounts` | 기존 계좌 비활성화 |
| **입금** | `POST` | `/transactions/deposit` | 계좌 입금 |
| **출금** | `POST` | `/transactions/withdraw` | 계좌 출금 (일일 한도: 100만원) |
| **이체** | `POST` | `/transactions/transfer` | 계좌 간 이체 (일일 한도: 300만원, 수수료: 1%) |
| **거래내역 조회** | `GET` | `/transactions/account/{bankCode}/{accountNo}` | 거래 내역 조회 (페이징, 기간 필터) |

### API 요청/응답 예시

#### 계좌 등록
**요청**
```http
POST /accounts
Content-Type: application/json

{
  "userName": "홍길동",
  "email": "hong@example.com",
  "idCardNo": "123456-1234567",
  "bankCode": "001",
  "accountNo": "123-456-789"
}
```

**응답**
```json
{
  "id": 1,
  "userId": 1,
  "bankCode": "001",
  "accountNo": "123-456-789",
  "balance": 0,
  "status": "ACTIVE",
  "createdAt": "2025-09-20T10:00:00",
  "deactivatedAt": null
}
```

#### 계좌 삭제
**요청**
```http
DELETE /accounts
Content-Type: application/json

{
  "bankCode": "001",
  "accountNo": "123-456-789"
}
```

**응답**
```
성공시 204 No Content
```

#### 입금
**요청**
```http
POST /transactions/deposit
Content-Type: application/json

{
  "bankCode": "001",
  "accountNo": "123-456-789",
  "amount": 50000,
  "description": "급여입금"
}
```

**응답**
```json
{
  "transactionId": 1,
  "accountInfo": {
    "bankCode": "001",
    "accountNo": "123-456-789"
  },
  "relatedAccountInfo": null,
  "transactionType": "DEPOSIT",
  "amount": 50000,
  "balanceAfter": 50000,
  "description": "급여입금",
  "createdAt": "2025-09-20T10:30:00",
  "fee": 0
}
```

#### 출금
**요청**
```http
POST /transactions/withdraw
Content-Type: application/json

{
  "bankCode": "001",
  "accountNo": "123-456-789",
  "amount": 30000,
  "description": "ATM 출금"
}
```

**응답**
```json
{
  "transactionId": 2,
  "accountInfo": {
    "bankCode": "001",
    "accountNo": "123-456-789"
  },
  "relatedAccountInfo": null,
  "transactionType": "WITHDRAW",
  "amount": 30000,
  "balanceAfter": 20000,
  "description": "ATM 출금",
  "createdAt": "2025-09-20T11:00:00",
  "fee": 0
}
```

#### 이체
**요청**
```http
POST /transactions/transfer
Content-Type: application/json

{
  "fromBankCode": "001",
  "fromAccountNo": "123-456-789",
  "toBankCode": "002",
  "toAccountNo": "987-654-321",
  "amount": 100000,
  "description": "생일축하금"
}
```

**응답**
```json
{
  "transactionId": 3,
  "accountInfo": {
    "bankCode": "001",
    "accountNo": "123-456-789"
  },
  "relatedAccountInfo": {
    "bankCode": "002",
    "accountNo": "987-654-321"
  },
  "transactionType": "TRANSFER",
  "amount": 100000,
  "balanceAfter": 150000,
  "description": "생일축하금",
  "createdAt": "2025-09-20T12:00:00",
  "fee": 1000
}
```

#### 거래내역 조회
**요청**
```http
GET /transactions/account/001/123456789?startDate=2024-01-01&endDate=2024-01-31&page=0&size=20
```

**응답**
```json
{
  "accountInfo": {
    "userName": "홍길동",
    "email": "hong@example.com",
    "balance": 150000,
    "bankCode": "001",
    "accountNo": "123-456-789"
  },
  "transactions": [
    {
      "transactionId": 3,
      "accountInfo": {
        "bankCode": "001",
        "accountNo": "123-456-789"
      },
      "relatedAccountInfo": {
        "bankCode": "002",
        "accountNo": "987-654-321"
      },
      "transactionType": "TRANSFER",
      "amount": 100000,
      "balanceAfter": 150000,
      "description": "생일축하금",
      "createdAt": "2025-09-21T12:00:00",
      "fee": 1000
    },
    {
      "transactionId": 2,
      "accountInfo": {
        "bankCode": "001",
        "accountNo": "123-456-789"
      },
      "relatedAccountInfo": null,
      "transactionType": "WITHDRAW",
      "amount": 30000,
      "balanceAfter": 50000,
      "description": "ATM 출금",
      "createdAt": "2025-09-20T11:00:00",
      "fee": 0
    }
  ],
  "pageInfo": {
    "currentPage": 0,
    "pageSize": 20,
    "totalElements": 2,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

### API 문서화
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html

## 비즈니스 규칙

### 거래 한도
- **출금 일일 한도**: 1,000,000원 (백만원)
- **이체 일일 한도**: 3,000,000원 (삼백만원)

### 수수료 정책
- **이체 수수료**: 이체 금액의 1% (소수점 반올림)

### 검증 규칙
- **계좌번호**: 10-14자리 숫자 (하이픈 허용: `123-456-789`)
- **주민번호**: 13자리 숫자 (하이픈 허용: `123456-1234567`)
- **이메일**: RFC 5322 표준 형식
- **금액**: 양수, 원 단위 (소수점 불허)

## 동시성 제어

### 다층 동시성 제어 전략

**금융 서비스의 특성**을 고려하여 다음과 같은 **다층 동시성 제어 메커니즘**을 구현하여 **데이터 일관성**과 **동시성 성능**을 보장합니다.

#### 1. **Optimistic Locking (낙관적 락)**
**정의**: JPA의 `@Version` 어노테이션을 활용한 낙관적 동시성 제어

**적용 엔티티**:
- `AccountJpaEntity` - 계좌 잔액 변경 시 버전 충돌 감지
- `DailyLimitJpaEntity` - 일일 한도 사용량 업데이트 시 충돌 방지
- `TransactionJpaEntity` - 거래 생성 시 무결성 보장

```java
@Entity
public class AccountJpaEntity {
    @Version
    private Integer version = 0;  // 자동 버전 관리

    // 엔티티 수정 시 version이 자동으로 증가
    // 동시 수정 시 OptimisticLockException 발생
}
```

**장점**: 읽기 성능이 우수하며, 충돌이 적은 상황에서 효율적

#### 2. **Pessimistic Locking (비관적 락)**
**정의**: JPA의 `LockModeType.PESSIMISTIC_WRITE`를 사용한 배타적 락

**구현 방식**:
```java
// JpaAccountPort 구현
@Override
public Optional<Account> findByIdWithLock(Long id) {
    AccountJpaEntity entity = entityManager.find(
        AccountJpaEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    return Optional.ofNullable(entity).map(this::mapToDomain);
}
```

**데드락 방지 전략**:
```java
// TransferUseCase - ID 기반 순서 락킹으로 데드락 방지
if (fromAccount.getId() < toAccount.getId()) {
    fromAccount = accountPort.findByIdWithLock(fromAccount.getId());
    toAccount = accountPort.findByIdWithLock(toAccount.getId());
} else {
    toAccount = accountPort.findByIdWithLock(toAccount.getId());
    fromAccount = accountPort.findByIdWithLock(fromAccount.getId());
}
```

**적용 시나리오**:
- 계좌 잔액 변경 (입금/출금/이체)
- 일일 한도 사용량 업데이트
- 높은 경합이 예상되는 중요 비즈니스 로직

#### 3. **Transaction Isolation & Atomicity**
**트랜잭션 범위**: `@Transactional` 을 통한 원자적 실행 보장

```java
@Service
@Transactional  // 클래스 레벨 트랜잭션
public class TransferUseCase {

    public TransactionResponse execute(TransferRequest request) {
        // 1. 일일 한도 체크 및 락
        validateAndLockDailyLimit(fromAccount.getId(), amount);

        // 2. 계좌 락 (데드락 방지 순서)
        // 3. 잔액 변경 (원자적 실행)
        fromAccount.withdraw(totalDeduction);
        toAccount.deposit(amount);

        // 4. 영속성 반영
        accountPort.save(fromAccount);
        accountPort.save(toAccount);

        // 5. 거래 기록 생성
        // 모든 작업이 하나의 트랜잭션에서 실행됨
    }
}
```

#### 4. **Lock Ordering Strategy (락 순서 정렬)**
**목적**: 데드락 방지를 위한 일관된 락 순서 보장

**구현**:
- **계좌 락**: ID가 작은 계좌부터 락 획득
- **일일 한도 락**: 계좌 락 이전에 먼저 획득
- **일관된 순서**: 모든 UseCase에서 동일한 락 순서 적용

```java
// 1단계: 일일 한도 락 (가장 먼저)
DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDateWithLock(
    accountId, today);

// 2단계: 계좌 락 (ID 순서 정렬)
if (fromAccountId < toAccountId) {
    lockFromFirst();
} else {
    lockToFirst();
}
```

#### 5. **Database-Level Constraints**
**데이터베이스 제약조건을 통한 최종 무결성 보장**

- **Unique Constraints**: 중복 계좌번호, 이메일 방지
- **Foreign Key Constraints**: 참조 무결성 보장
- **Check Constraints**: 잔액 음수 방지 등 비즈니스 규칙
- **Database Isolation Level**: READ_COMMITTED 사용

#### 6. **동시성 시나리오별 대응 전략**

| 시나리오 | 동시성 제어 방법 | 성능 최적화 |
|---------|----------------|------------|
| **단일 계좌 입금** | Pessimistic Lock | 빠른 락 해제 |
| **단일 계좌 출금** | Pessimistic Lock + 일일한도 락 | 락 순서 최적화 |
| **계좌간 이체** | 정렬된 Pessimistic Lock | ID 기반 데드락 방지 |
| **거래내역 조회** | Lock-free 읽기 | Optimistic Locking |

## 테스트 전략

### 포괄적인 테스트 커버리지

**금융 시스템의 안정성**을 보장하기 위해 다음과 같은 **다층 테스트 전략**을 구현

#### 테스트 유형별 커버리지
- **Unit Tests** (단위 테스트): 비즈니스 로직 검증
- **Integration Tests** (통합 테스트): API 엔드포인트 검증
- **Concurrency Tests** (동시성 테스트): 다중 스레드 환경 검증
- **Repository Tests** (저장소 테스트): 데이터 접근 계층 검증

#### 주요 테스트 시나리오
- **계좌 생성/삭제** (중복 검증, 데이터 무결성)
- **입출금 처리** (잔액 검증, 상태 관리)
- **이체 처리** (수수료 계산, 한도 검증, 동시성)
- **일일 한도 추적** (한도 초과 방지, 롤백 처리)
- **동시성 시나리오** (락 메커니즘, 데드락 방지)
- **예외 상황 처리** (잘못된 요청, 시스템 오류)

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew money-transfer-application:test

# 동시성 테스트만 실행
./gradlew test --tests "*ConcurrencyTest"
```

## 빌드 및 실행

### 사전 요구사항
- **Java 17** 이상
- **Docker** 및 **Docker Compose**
- **Git**

### 1. 프로젝트 클론
```bash
git clone 'https://github.com/ssipflow/KimMyeongGyun_backend.git'
cd KimMyeongGyun_backend.git
```

### 2. Docker Compose 실행 (권장)

**운영 환경과 동일한 MySQL 환경으로 실행:**

```bash
# Docker 컨테이너 빌드 및 시작
docker-compose up --build

# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f money-transfer-api

# 서비스 중지
docker-compose down
```

### 3. 로컬 개발 환경 실행

**H2 인메모리 데이터베이스로 빠른 개발/테스트:**

```bash
# Gradle 래퍼를 사용한 빌드
./gradlew build

# API 서버 실행 (포트: 8080)
./gradlew money-transfer-api:bootRun
```

### 서비스 확인
- **API Server(Docker)**: http://localhost:8081
- **Swagger UI(DOcker)**: http://localhost:8081/swagger-ui/index.html
- **MySQL** (Docker 사용시): localhost:3307

### Docker 구성 정보
- **MySQL**: 포트 3307, 사용자명/비밀번호: moneyuser/moneypass
- **Application**: 포트 8081
- **Network**: 사용자 정의 네트워크로 서비스 간 통신