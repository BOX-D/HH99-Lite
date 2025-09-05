# 콘서트 예약 서비스 ERD (Entity Relationship Diagram)

## 개요

콘서트 예약 서비스의 데이터베이스 설계도입니다. 대기열 시스템, 예약 관리, 결제 처리를 위한 테이블 구조를 정의합니다. 동시성 이슈를 고려하여 구현하며, 다수의 인스턴스로 애플리케이션이 동작하더라도 기능에 문제가 없도록 설계되었습니다.

## 테이블 구조

### 1. users (사용자)
사용자 기본 정보를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PRIMARY KEY | 사용자 UUID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 사용자명 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

### 2. user_balances (사용자 잔액)
사용자의 잔액 정보를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 잔액 ID |
| user_id | VARCHAR(36) | NOT NULL, FOREIGN KEY | 사용자 ID |
| balance | DECIMAL(10,2) | NOT NULL, DEFAULT 0 | 현재 잔액 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

### 3. balance_transactions (잔액 거래 내역)
잔액 충전/사용 내역을 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 거래 ID |
| user_id | VARCHAR(36) | NOT NULL, FOREIGN KEY | 사용자 ID |
| transaction_type | ENUM('CHARGE', 'PAYMENT', 'REFUND') | NOT NULL | 거래 유형 |
| amount | DECIMAL(10,2) | NOT NULL | 거래 금액 |
| balance_before | DECIMAL(10,2) | NOT NULL | 거래 전 잔액 |
| balance_after | DECIMAL(10,2) | NOT NULL | 거래 후 잔액 |
| description | VARCHAR(200) | NULL | 거래 설명 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 거래일시 |

### 4. concerts (콘서트)
콘서트 정보를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 콘서트 ID |
| name | VARCHAR(100) | NOT NULL | 콘서트명 |
| date | DATE | NOT NULL | 공연일 |
| total_seats | INT | NOT NULL | 총 좌석 수 |
| price_per_seat | DECIMAL(10,2) | NOT NULL | 좌석당 가격 |
| status | ENUM('ACTIVE', 'CANCELLED', 'COMPLETED') | NOT NULL, DEFAULT 'ACTIVE' | 콘서트 상태 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

### 5. seats (좌석)
좌석 정보를 관리합니다. 좌석 번호는 1부터 50까지로 관리됩니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 좌석 ID |
| concert_id | BIGINT | NOT NULL, FOREIGN KEY | 콘서트 ID |
| seat_number | INT | NOT NULL, CHECK (seat_number BETWEEN 1 AND 50) | 좌석 번호 (1-50) |
| status | ENUM('AVAILABLE', 'RESERVED', 'TEMPORARY_HOLD', 'PAID') | NOT NULL, DEFAULT 'AVAILABLE' | 좌석 상태 |
| locked_until | TIMESTAMP | NULL | 임시 배정 만료 시간 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

### 6. queue_tokens (대기열 토큰)
대기열 토큰 정보를 관리합니다. 유저의 UUID와 해당 유저의 대기열을 관리할 수 있는 정보를 포함합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 토큰 ID |
| token | VARCHAR(255) | NOT NULL, UNIQUE | 토큰 값 |
| user_id | VARCHAR(36) | NOT NULL, FOREIGN KEY | 사용자 ID |
| queue_position | INT | NOT NULL | 대기열 순서 |
| estimated_wait_time | INT | NOT NULL | 예상 대기 시간(초) |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | 활성 상태 |
| expires_at | TIMESTAMP | NOT NULL | 만료일시 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

### 7. reservations (예약)
좌석 예약 정보를 관리합니다. 좌석 예약과 동시에 해당 좌석은 그 유저에게 약 5분간 임시 배정됩니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PRIMARY KEY | 예약 UUID |
| user_id | VARCHAR(36) | NOT NULL, FOREIGN KEY | 사용자 ID |
| concert_id | BIGINT | NOT NULL, FOREIGN KEY | 콘서트 ID |
| seat_id | BIGINT | NOT NULL, FOREIGN KEY | 좌석 ID |
| status | ENUM('TEMPORARY_HOLD', 'PAID', 'EXPIRED') | NOT NULL, DEFAULT 'TEMPORARY_HOLD' | 예약 상태 |
| expires_at | TIMESTAMP | NOT NULL | 만료일시 (임시 배정 시간) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

### 8. payments (결제)
결제 정보를 관리합니다. 결제가 완료되면 해당 좌석의 소유권을 유저에게 배정하고 대기열 토큰을 만료시킵니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | VARCHAR(36) | PRIMARY KEY | 결제 UUID |
| reservation_id | VARCHAR(36) | NOT NULL, FOREIGN KEY | 예약 ID |
| user_id | VARCHAR(36) | NOT NULL, FOREIGN KEY | 사용자 ID |
| amount | DECIMAL(10,2) | NOT NULL | 결제 금액 |
| status | ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') | NOT NULL, DEFAULT 'PENDING' | 결제 상태 |
| payment_method | VARCHAR(50) | NOT NULL, DEFAULT 'BALANCE' | 결제 방법 (잔액 기반) |
| completed_at | TIMESTAMP | NULL | 완료일시 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

## 관계도

```
users (1) ←→ (1) user_balances
users (1) ←→ (N) balance_transactions
users (1) ←→ (N) queue_tokens
users (1) ←→ (N) reservations
users (1) ←→ (N) payments

concerts (1) ←→ (N) seats
concerts (1) ←→ (N) reservations

seats (1) ←→ (N) reservations

reservations (1) ←→ (1) payments
```

## 인덱스

### 성능 최적화를 위한 인덱스

1. **users**
   - `idx_users_email` (email)
   - `idx_users_username` (username)

2. **user_balances**
   - `idx_user_balances_user_id` (user_id)

3. **balance_transactions**
   - `idx_balance_transactions_user_id` (user_id)
   - `idx_balance_transactions_created_at` (created_at)

4. **concerts**
   - `idx_concerts_date` (date)
   - `idx_concerts_status` (status)

5. **seats**
   - `idx_seats_concert_id` (concert_id)
   - `idx_seats_status` (status)
   - `idx_seats_concert_seat` (concert_id, seat_number)

6. **queue_tokens**
   - `idx_queue_tokens_token` (token)
   - `idx_queue_tokens_user_id` (user_id)
   - `idx_queue_tokens_is_active` (is_active)
   - `idx_queue_tokens_expires_at` (expires_at)

7. **reservations**
   - `idx_reservations_user_id` (user_id)
   - `idx_reservations_concert_id` (concert_id)
   - `idx_reservations_seat_id` (seat_id)
   - `idx_reservations_status` (status)
   - `idx_reservations_expires_at` (expires_at)

8. **payments**
   - `idx_payments_reservation_id` (reservation_id)
   - `idx_payments_user_id` (user_id)
   - `idx_payments_status` (status)
   - `idx_payments_created_at` (created_at)

## 제약조건

### 외래키 제약조건
- `user_balances.user_id` → `users.id`
- `balance_transactions.user_id` → `users.id`
- `queue_tokens.user_id` → `users.id`
- `reservations.user_id` → `users.id`
- `reservations.concert_id` → `concerts.id`
- `reservations.seat_id` → `seats.id`
- `payments.reservation_id` → `reservations.id`
- `payments.user_id` → `users.id`
- `seats.concert_id` → `concerts.id`

### 비즈니스 규칙
1. 한 사용자는 동시에 하나의 활성 대기열 토큰만 가질 수 있습니다.
2. 한 좌석은 동시에 하나의 예약만 가질 수 있습니다.
3. 예약은 결제 완료 시에만 확정됩니다.
4. 잔액은 음수가 될 수 없습니다.
5. 좌석 번호는 1부터 50까지로 관리되며, 콘서트별로 중복되지 않습니다.
6. 좌석 예약 시 5분간 임시 배정되며, 이 시간 내에 결제가 완료되지 않으면 자동으로 해제됩니다.
7. 동시에 여러 사용자가 예약 요청을 했을 때, 좌석이 중복으로 배정되지 않도록 합니다.
8. 결제 완료 시 대기열 토큰이 만료됩니다.
9. 각 기능 및 제약사항에 대해 단위 테스트를 반드시 하나 이상 작성합니다.
