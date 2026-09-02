# E-commerce

Backend e-commerce sử dụng Spring Boot, PostgreSQL và Gradle.

## Yêu cầu

- JDK 25
- Docker và Docker Compose

## Chuẩn bị database lần đầu

Project sử dụng PostgreSQL chạy bằng Docker. Từ thư mục gốc của project, chạy:

```bash
docker compose -f docker/docker-compose.yaml up -d
```

Docker sẽ tạo database, schema `ecommerce` và volume lưu dữ liệu. Chỉ cần chạy lại lệnh trên khi container PostgreSQL đang dừng.

Cấu hình kết nối database nằm trong `environment/application.yaml`.

## Chạy development

```bash
./scripts/run.sh
```

Script sẽ build và chạy ứng dụng. Sau khi khởi động thành công:

- API: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- Remote debug: `127.0.0.1:8081`

Trong các lần làm việc tiếp theo, nếu PostgreSQL vẫn đang chạy thì chỉ cần chạy `./scripts/run.sh`.

## Build và test

```bash
./gradlew build
```