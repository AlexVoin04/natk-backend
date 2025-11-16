# 🧩 NATK Backend

This repository contains a microservices architecture with:

- **natk-api** — Main application API
- **natk-auth** — Authentication and JWT handling service
- **natk-pdf** — PDF conversion microservice
- **natk-ai** — AI service integrating Gemini API
- **PostgreSQL** — Database for persistence
- **MinIO** — S3-compatible storage that stores files uploaded through NATK services

---
# 📦 MinIO (File Storage)
- Stores user files (PDF, images, documents)
- Uses `natk-api` to work with files (uploading files, returning a link to a file in a bucket)
- Fully compatible with the Amazon S3 API
---

# NATK API
### Requirements
- Java 23
- Docker

---

## 🐳 How to Build and Run Dev Environment with Docker Compose

Copy `.env.example` to `.env` then run:

### ✅ Option 1: Using `docker-compose` with auto build

From the root of the project, run:

```shell
docker-compose -f docker-compose.yml up --build
```

This will:

- Build Docker images for `natk-auth`, `natk-api`, `natk-pdf`and `natk-ai`
- Start the `natk-auth`, `natk-api`, `natk-pdf`, `natk-ai`, `minio`, and `db` containers
- Automatically create a shared network `natk-net`

---

### If you want to run only specific services
Replace `db natk-api natk-ai` with the services you need to run:
```shell
docker compose -f docker-compose.yml up --no-deps db natk-api natk-ai
```

___

### 🛠 Option 2: Manually Build Docker Images
Build images manually:

🔐 Build natk-auth image
```shell
docker build -t natk-auth:0.0.1 ./natk-auth
```

🌐 Build natk-api image
```shell
docker build -t natk-api:0.0.9 ./natk-api
```

📄 Build natk-pdf image
```shell
docker build -t natk-pdf:0.0.1 ./natk-pdf
```

🤖 Build natk-ai image
```shell
docker build -t natk-ai:0.0.3 ./natk-ai
```

▶️ Run the containers
```shell
docker-compose -f docker-compose.yml up
```

---

## 🔌 Default Ports

| Service       | Host Port | Container Port |
|---------------|-----------|----------------|
| PostgreSQL    | 5432      | 5432           |
| natk-auth     | 8001      | 8080           |
| natk-api      | 8000      | 8080           |
| natk-ai       | 8002      | 8080           |
| natk-pdf      | 8003      | 8080           |
| MinIO API     | 9000      | 9000           |
| MinIO Console | 9001      | 9001           |

---

## 📣 After Running

Once started, you should see:

- PostgreSQL initialized and ready
- `natk-auth` running at http://localhost:8001
- `natk-api` running at http://localhost:8000
- `natk-ai` running at http://localhost:8002
- `natk-pdf` running at http://localhost:8003

You can now interact with the services via their respective endpoints.

---