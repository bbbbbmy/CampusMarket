# CampusMarket 单 image Dockerfile（v0.1.2 单进程）
# 入口默认 java -jar campus-app-0.1.0-SNAPSHOT.jar；v0.2.x 会拆 5 image + 网关
#
# 本地构建：
#   mvn -B -ntp -f backend/pom.xml -DskipTests install
#   docker build -t campus-market:local .
#
# GitHub Actions 自动推 GHCR：
#   详见 .github/workflows/release.yml
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="CampusMarket"
LABEL org.opencontainers.image.description="校园二手市场后端 — Spring Boot 3.3 + JPA + H2 demo"
LABEL org.opencontainers.image.source="https://github.com/bbbbbmy/CampusMarket"
LABEL org.opencontainers.image.licenses="MIT"

# 非 root 运行
RUN addgroup -S app && adduser -S app -G app
WORKDIR /opt/campus
USER app

COPY backend/campus-app/target/campus-app-0.1.0-SNAPSHOT.jar /opt/campus/app.jar

EXPOSE 8080

# 健康检查：Spring Boot Actuator 标准端点（v0.1 默认无 actuator，curl / 简单 200 即可）
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/v1/schools >/dev/null 2>&1 || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=70.0", "-jar", "/opt/campus/app.jar"]