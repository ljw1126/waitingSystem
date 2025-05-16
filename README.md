# 접속 대기열 시스템

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_waitingSystem&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ljw1126_waitingSystem)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_waitingSystem&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=ljw1126_waitingSystem)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_waitingSystem&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ljw1126_waitingSystem)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ljw1126_waitingSystem&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ljw1126_waitingSystem)

## 프로젝트 목표
대량 트래픽 상황에서도 안정적인 사용자 경험 제공을 위한 대기열 시스템 개발
- 비동기 논블로킹 기반의 Spring Webflux, Reactive Redis를 활용해 시스템 설계
- JMeter를 통한 테스트 및 병목 지점 분석과 튜닝
<br/>

## 요구사항 
1. 웹 페이지에 동시 진입할 수 있는 사용자 수를 제한한다.
2. 대기열에 사용자를 등록하고, 순차적으로 진입을 허용한다.
3. 대기 중인 사용자에게 현재 대기열 순위를 주기적으로 제공한다.

<br/>

## 개발 스택
>- Server: JDK 17, Spring Boot 3.x, Webflux, Reactive Redis <br/>
>- Database: Redis <br/>
>- Testing: JUnit5, Mockito, Reactor, JMeter <br/>
>- Etc: Git, Docker, Gradle, Ubuntu, jacoco, SonarCloud <br/>

<br/>

## 아키텍처
<img src="https://github.com/ljw1126/user-content/blob/master/waiting-system/flow.jpg?raw=true" alt="다이어그램" style="float: left" />
<br/>

## 테스트 결과
**Apache JMeter** 사용
- Number of Threads (users): `1,000`
- Ramp-up period (seconds): `10`
- Loop Count: `infinite`

테스트 환경 분리 후 **Throughput이 약 65배 (297.6 → 19,332.1)** 향상

| 환경                  | Samples   | Min (ms) | Max (ms) | Avg (ms) | Error (%) | Throughput (QPS) |
|---------------------|-----------|----------|----------|----------|-----------|------------------|
| 초기 테스트<br/> (잡음 포함) | 887,063   | 0        | 256      | 49       | 0.11      | 297.6            |
| 환경 분리 후 테스트         | 1,016,596 | 0        | 185      | 46       | 0.10      | **19332.1**      |
