# 콘서트 예약 서비스 API 명세서

## 개요

콘서트 예약 서비스의 REST API 명세서입니다. 대기열 시스템을 구축하고, 예약 서비스는 작업가능한 유저만 수행할 수 있도록 합니다. 사용자는 좌석예약 시에 미리 충전한 잔액을 이용하며, 좌석 예약 요청시에 결제가 이루어지지 않더라도 일정 시간동안 다른 유저가 해당 좌석에 접근할 수 없도록 합니다.

## 주요 특징

- **대기열 시스템**: 모든 API는 대기열 토큰 검증을 통해 접근 제어
- **동시성 제어**: 다수의 인스턴스로 동작하더라도 기능에 문제 없도록 구현
- **임시 배정**: 좌석 예약 시 5분간 임시 배정 (정책에 따라 조정 가능)
- **잔액 기반 결제**: 미리 충전한 잔액을 이용한 결제 시스템

## 공통 사항

### Base URL
```
http://localhost:8080/api/v1
```

### 인증
- 모든 API 요청 시 `Authorization` 헤더에 대기열 토큰을 포함해야 합니다.
- 토큰 형식: `Bearer {token}`

### 응답 형식
```json
{
  "success": true,
  "data": {},
  "message": "성공적으로 처리되었습니다."
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

## 1. 유저 대기열 토큰 API (주요)

### 1.1 토큰 발급
서비스를 이용할 토큰을 발급받는 API입니다. 토큰은 유저의 UUID와 해당 유저의 대기열을 관리할 수 있는 정보(대기 순서, 잔여 시간 등)를 포함합니다.

**POST** `/queue/token`

#### Request Body
```json
{
  "userId": "user-uuid-string"
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "token": "queue-token-string",
    "queuePosition": 15,
    "estimatedWaitTime": 300,
    "expiresAt": "2024-01-15T10:30:00Z"
  }
}
```

#### 비즈니스 규칙
- 기본적으로 폴링으로 본인의 대기열을 확인한다고 가정
- 다양한 전략을 통해 합리적으로 대기열을 제공 (특정 시간 동안 N명에게만 권한 부여, 한번에 활성화된 최대 유저를 N으로 유지 등)
- 유저간 대기열을 요청 순서대로 정확하게 제공

### 1.2 대기열 상태 조회
현재 대기열 상태를 조회합니다.

**GET** `/queue/status`

#### Response
```json
{
  "success": true,
  "data": {
    "queuePosition": 15,
    "estimatedWaitTime": 300,
    "isActive": true,
    "expiresAt": "2024-01-15T10:30:00Z"
  }
}
```

## 2. 예약 가능 날짜/좌석 API (기본)

### 2.1 예약 가능 날짜 조회
예약가능한 날짜와 해당 날짜의 좌석을 조회하는 API입니다. 예약 가능한 날짜 목록을 조회할 수 있습니다.

**GET** `/concerts/dates`

#### Response
```json
{
  "success": true,
  "data": {
    "dates": [
      {
        "date": "2024-02-15",
        "concertName": "Spring Concert 2024",
        "availableSeats": 45,
        "totalSeats": 50
      },
      {
        "date": "2024-02-20",
        "concertName": "Jazz Night",
        "availableSeats": 30,
        "totalSeats": 50
      }
    ]
  }
}
```

### 2.2 예약 가능 좌석 조회
날짜 정보를 입력받아 예약가능한 좌석정보를 조회할 수 있습니다. 좌석 정보는 1 ~ 50 까지의 좌석번호로 관리됩니다.

**GET** `/concerts/{date}/seats`

#### Path Parameters
- `date`: 콘서트 날짜 (YYYY-MM-DD)

#### Response
```json
{
  "success": true,
  "data": {
    "date": "2024-02-15",
    "concertName": "Spring Concert 2024",
    "seats": [
      {
        "seatNumber": 1,
        "status": "AVAILABLE"
      },
      {
        "seatNumber": 2,
        "status": "RESERVED"
      },
      {
        "seatNumber": 3,
        "status": "TEMPORARY_HOLD"
      }
    ]
  }
}
```

## 3. 좌석 예약 요청 API (주요)

### 3.1 좌석 예약 요청
날짜와 좌석 정보를 입력받아 좌석을 예약 처리하는 API입니다. 좌석 예약과 동시에 해당 좌석은 그 유저에게 약 5분간 임시 배정됩니다. 만약 배정 시간 내에 결제가 완료되지 않는다면 좌석에 대한 임시 배정은 해제되어야 하며 다른 사용자는 예약할 수 없어야 합니다.

**POST** `/reservations`

#### 비즈니스 규칙
- 동시에 여러 사용자가 예약 요청을 했을 때, 좌석이 중복으로 배정 가능하지 않도록 함
- 임시 배정 시간은 정책에 따라 자율적으로 정의 가능 (기본 5분)

#### Request Body
```json
{
  "date": "2024-02-15",
  "seatNumber": 5
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "reservationId": "reservation-uuid",
    "date": "2024-02-15",
    "seatNumber": 5,
    "status": "TEMPORARY_HOLD",
    "expiresAt": "2024-01-15T10:35:00Z",
    "price": 50000
  }
}
```

### 3.2 예약 상태 조회
현재 예약 상태를 조회합니다.

**GET** `/reservations/{reservationId}`

#### Response
```json
{
  "success": true,
  "data": {
    "reservationId": "reservation-uuid",
    "date": "2024-02-15",
    "seatNumber": 5,
    "status": "TEMPORARY_HOLD",
    "expiresAt": "2024-01-15T10:35:00Z",
    "price": 50000
  }
}
```

## 4. 잔액 충전/조회 API (기본)

### 4.1 잔액 충전
결제에 사용될 금액을 API를 통해 충전하는 API입니다. 사용자 식별자 및 충전할 금액을 받아 잔액을 충전합니다.

**POST** `/users/balance/charge`

#### Request Body
```json
{
  "amount": 100000
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "userId": "user-uuid",
    "balance": 150000,
    "chargedAmount": 100000,
    "transactionId": "transaction-uuid"
  }
}
```

### 4.2 잔액 조회
사용자 식별자를 통해 해당 사용자의 잔액을 조회합니다.

**GET** `/users/balance`

#### Response
```json
{
  "success": true,
  "data": {
    "userId": "user-uuid",
    "balance": 150000
  }
}
```

## 5. 결제 API (주요)

### 5.1 결제 처리
결제 처리하고 결제 내역을 생성하는 API입니다. 결제가 완료되면 해당 좌석의 소유권을 유저에게 배정하고 대기열 토큰을 만료시킵니다.

**POST** `/payments`

#### Request Body
```json
{
  "reservationId": "reservation-uuid"
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "paymentId": "payment-uuid",
    "reservationId": "reservation-uuid",
    "amount": 50000,
    "status": "COMPLETED",
    "completedAt": "2024-01-15T10:32:00Z"
  }
}
```

## 심화 과제

### 6. 대기열 고도화 (심화)
다양한 전략을 통해 합리적으로 대기열을 제공할 방법을 고안합니다.

#### 구현 방안 예시
- 특정 시간 동안 N명에게만 권한을 부여
- 한번에 활성화된 최대 유저를 N으로 유지
- 유저간 대기열을 요청 순서대로 정확하게 제공

## 에러 코드

| 코드 | 설명 |
|------|------|
| `INVALID_TOKEN` | 유효하지 않은 토큰 |
| `TOKEN_EXPIRED` | 만료된 토큰 |
| `QUEUE_NOT_ACTIVE` | 비활성 대기열 |
| `SEAT_NOT_AVAILABLE` | 예약 불가능한 좌석 |
| `RESERVATION_EXPIRED` | 만료된 예약 |
| `INSUFFICIENT_BALANCE` | 잔액 부족 |
| `RESERVATION_NOT_FOUND` | 예약 정보 없음 |
| `PAYMENT_ALREADY_COMPLETED` | 이미 완료된 결제 |
| `INVALID_DATE` | 유효하지 않은 날짜 |
| `INVALID_SEAT_NUMBER` | 유효하지 않은 좌석 번호 |
| `CONCURRENT_RESERVATION` | 동시 예약 시도로 인한 충돌 |
| `QUEUE_FULL` | 대기열이 가득 참 |

## 상태 코드

| 상태 | 설명 |
|------|------|
| `AVAILABLE` | 예약 가능 |
| `RESERVED` | 예약 완료 |
| `TEMPORARY_HOLD` | 임시 배정 (5분) |
| `PAID` | 결제 완료 |
| `EXPIRED` | 만료됨 |
