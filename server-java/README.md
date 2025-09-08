# 콘서트 예약 서비스 (Concert Reservation Service)

## 프로젝트 개요

대기열 시스템을 기반으로 한 콘서트 좌석 예약 서비스입니다. 사용자는 대기열 토큰을 발급받아 예약 가능한 좌석을 조회하고, 임시 배정된 좌석에 대해 결제를 완료하여 최종 예약을 확정할 수 있습니다.

## 주요 기능

- 🔑 **대기열 토큰 시스템**: 유저별 대기열 관리 및 토큰 발급
- 📅 **예약 가능 날짜/좌석 조회**: 콘서트 일정 및 좌석 현황 조회
- 🎫 **좌석 예약**: 임시 배정 시스템 (5분 타임아웃)
- 💰 **잔액 관리**: 충전 및 조회 기능
- 💳 **결제 처리**: 최종 예약 확정 및 토큰 만료

## 기술 스택

- **Backend**: Spring Boot 3.4.1, Java 17
- **Database**: MySQL 8.0
- **Cache/Queue**: Redis
- **Build Tool**: Gradle
- **Container**: Docker, Docker Compose

## 서버 설계 문서

- [API 명세서](./docs/api-specification.md)
- [ERD](./docs/erd.md)
- [인프라 구성도](./docs/infrastructure.md)

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
```