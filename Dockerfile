# ============================================
# Stage 1: 构建 Vue 前端
# ============================================
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --only=production && npm cache clean --force
COPY frontend/ .
RUN npm run build

# ============================================
# Stage 2: 构建 Java 后端（远程 SSH 监控）
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app

COPY java-backend/pom.xml ./
RUN mvn dependency:go-offline -q 2>/dev/null || true

COPY java-backend/src ./src
# 前端构建产物嵌入 Spring Boot 静态资源目录
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static

RUN mvn package -DskipTests -q && \
    mv target/go-monitor-1.0.0.jar /app/go-monitor.jar

# ============================================
# Stage 3: 运行镜像
# ============================================
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend-build /app/go-monitor.jar ./go-monitor.jar

EXPOSE 9500

# 挂载配置目录（可放入自定义 application.yml）
VOLUME ["/app/config"]

ENTRYPOINT ["java", "-jar", "go-monitor.jar"]
