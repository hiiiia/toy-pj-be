# syntax=docker/dockerfile:1

# ===== 1단계: 빌드 (JDK 포함 이미지) =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 정의 파일만 먼저 복사 → 소스만 바뀌면 의존성 다운로드 레이어는 캐시를 재사용
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
RUN ./gradlew bootJar -x test --no-daemon \
 && java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination extracted

# ===== 2단계: 실행 (JRE 만 포함한 가벼운 이미지) =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# root 가 아닌 전용 사용자로 실행. 첨부파일 저장 디렉터리는 이 사용자만 쓸 수 있게 한다
RUN groupadd --system app && useradd --system --gid app --no-create-home app \
 && mkdir -p /app/data/uploads && chown -R app:app /app/data

# 자주 바뀌지 않는 것부터 복사 (라이브러리 → 로더 → 우리 코드) → 코드만 바뀌면 마지막 레이어만 새로 받음
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    STORAGE_DIR=/app/data/uploads
# 컨테이너 기본 시간대는 UTC → 로그·시각이 한국 시간으로 찍히도록 JVM 시간대를 지정
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.launch.JarLauncher"]
