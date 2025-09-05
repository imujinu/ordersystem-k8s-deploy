FROM openjdk:17-jdk-alpine as stage1

WORKDIR /app

COPY gradle gradle 
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
RUN chmod +x gradlew
RUN ./gradlew bootJar

# 두번째 스테이지 FROM을 쓰면 새로운 스테이지가 열린다.
# 이미지 경량화를 위해 스테이지 분리
FROM openjdk:17-jdk-alpine
WORKDIR /app
# stage1의 jar 파일을 stage2로 copy
COPY --from=stage1 /app/build/libs/*.jar app.jar


# 실행 : CMD 또는 ENTRYPOINT를 통해 컨테이너 실행 
ENTRYPOINT ["java", "-jar", "app.jar"]

# 도커 이미지 빌드
# docker build -t ordersystem:v1.0 .
# 도커컨테이너 실행
# docker 내부에서 localhost를 찾는 설정은 루프백 문제 발생 
# docker run --name my-ordersystem -d -p 8080:8080 ordersystem:v1.0
# 도커 컨테이너 실행 시점에 docker.host.internal을 환경변수로 주입한다.
# docker run --name my-ordersystem -d -p 8080:8080 -e SPRING_REDIS_HOST=host.docker.internal -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/ordersystem ordersystem:v1.0