FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :modules:app:installDist --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/modules/app/build/install/app .
EXPOSE 8080
CMD ["bin/app"]
