# 🧩 NATK Backend

This repository contains a microservices architecture with:

- **natk-api** — Main application API
- **natk-auth** — Authentication and JWT handling service
- **natk-pdf** — PDF conversion microservice
- **natk-ai** — AI service integrating Gemini API
- **PostgreSQL** — Database for persistence
- **MinIO** — S3-compatible storage that stores files uploaded through NATK-API

---
# 📦 MinIO: S3-Compatible File Storage (with SSE-KMS Encryption)
NATK uses MinIO as a distributed S3-compatible storage for all uploaded user and department files.
It serves as a secure and scalable backend for:
- User storage (user-files)
- Department storage (department-files)

## 🔐 Server-Side Encryption (SSE-KMS)

MinIO is configured with server-side encryption (SSE-KMS) enabled. 
This means:
- All files are automatically encrypted before being written to disk
- Encryption uses a symmetric key defined in `.env`
- NATK services don’t need to manage encryption keys manually
- MinIO handles key rotation and internal cryptography

## ⚙️ MinIO Configuration in docker-compose.yml (SSE-KMS ON)
```yaml
environment:
  - MINIO_ROOT_USER=${MINIO_ROOT_USER}
  - MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
  # 🔐 Enable encrypted storage using internal KMS
  - MINIO_KMS_SECRET_KEYS=${MINIO_KMS_SECRET_KEYS}
  - MINIO_KMS_AUTO_ENCRYPTION=on
```

## 🔑 .env Example for Encryption
```yaml
# MinIO admin credentials
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123

# AES-256 master key for SSE-KMS
MINIO_KMS_SECRET_KEYS=my-minio-key:Lo4nNhprTgYkfJ9+Cn7KmV7cAnEUaDczTOoki6YCZXE=
```
## Requirements for the key format:


```markdown
<alias>:<base64-encoded-32-byte-key>
```


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