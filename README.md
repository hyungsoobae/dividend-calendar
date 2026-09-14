# 배당 달력 (Dividend Calendar)

미국 주식 배당 일정을 자동 수집해 달력으로 보여주고, 종목별 AI 해설을 제공하는 프로젝트.

- 설계: [docs/architecture.md](docs/architecture.md)
- FMP API 분석: [docs/fmp-dividends-notes.md](docs/fmp-dividends-notes.md)

## 기술 스택

- Java 21
- Spring Boot 4.1
- Spring Data JPA
- PostgreSQL 18
- Gradle (Kotlin DSL)

## 로컬 실행

### 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env`를 열어 값을 채운다. `POSTGRES_PASSWORD`는 직접 정하고, `FMP_API_KEY`는 [FMP](https://site.financialmodelingprep.com)에서 발급받는다.

### 2. PostgreSQL 실행

```bash
docker compose up -d
docker compose ps    # STATUS가 healthy 인지 확인
```

### 3. 애플리케이션 실행

```bash
set -a; source .env; set +a   # .env를 환경변수로 주입
./gradlew bootRun
```

### 정리

```bash
docker compose down      # 컨테이너 종료 (데이터 유지)
docker compose down -v   # 데이터까지 삭제
```

`.env`의 `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`는 컨테이너를 처음 만들 때만 반영된다. 변경하려면 `down -v`로 볼륨을 지우고 다시 생성해야 한다.