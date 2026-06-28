# 🧩 NATK Backend

This repository contains a microservices architecture with:

- **natk-api** — Main application API
- **natk-auth** — Authentication and JWT handling service
- **natk-pdf** — PDF conversion microservice
- **natk-ai** — AI service integrating Gemini API
- **natk-antivirus** — Antivirus scanner (ClamAV + RabbitMQ)
- **PostgreSQL** — Database for persistence
- **MinIO** — S3-compatible storage that stores files uploaded through NATK-API
- **RabbitMQ** — Queue broker used for antivirus scanning 
- **ClamAV** — Antivirus daemon used by natk-antivirus service

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
docker compose -f docker-compose.yml up
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
./gradlew :natk-auth:jibDockerBuild --image=natk-auth:0.2.0
```

🌐 Build natk-api image
```shell
./gradlew :natk-api:jibDockerBuild  --image=natk-api:0.7.2
```

📄 Build natk-pdf image
```shell
docker build -t natk-pdf:0.2.0 ./natk-pdf
```

🤖 Build natk-ai image
```shell
docker build -t natk-ai:0.1.0 ./natk-ai
```

🛡 Build natk-antivirus
```shell
./gradlew :natk-antivirus:jibDockerBuild --image=natk-antivirus:0.1.0
```

▶️ Run the containers
```shell
docker-compose -f docker-compose.yml up
```

---
# 🛡 Antivirus Architecture (RabbitMQ + ClamAV + natk-antivirus)

NATK uses a distributed antivirus scanning architecture based on RabbitMQ and ClamAV.

### 🔄 How Antivirus Workflow Works
1. **User uploads a file** to NATK-API → file is placed into the `incoming` MinIO bucket.
2. **natk-api** publishes a message into RabbitMQ queue `scan-file`.
3. **natk-antivirus** receives the message and:
   - downloads the file from MinIO
   - streams it to **ClamAV** for scanning
4. Based on scan result:
   - ✔ Clean → file is moved to final MinIO storage
   - ❌ Infected → file is deleted and marked as INFECTED
   - ⚠ Error → file is marked as ERROR
5. **natk-antivirus** notifies natk-api via internal REST callback.

This allows **fully asynchronous, fault-tolerant, and scalable** antivirus scanning.

---

### 📦 ClamAV Database (Important Note)

In this architecture, **FreshClam is disabled inside containers** (`CLAMAV_NO_FRESHCLAMD=true`) and the antivirus database is managed **locally**.

This is required because:
- FreshClam may be rate-limited (HTTP 429 / 403) by the ClamAV CDN
- CDN access can be unstable depending on network conditions
- The system is designed to be fully deterministic and offline-capable
- The virus database is mounted locally into the container (`./clamav/db:/var/lib/clamav`)

Because of this design, **the ClamAV database must be updated manually or via an external updater process**.

---

### ⬇️ Manual Database Updates

To keep virus definitions up to date, the following files must be downloaded manually:

- https://database.clamav.net/main.cvd
- https://database.clamav.net/daily.cvd
- https://database.clamav.net/bytecode.cvd

These files must be placed into:

```bash
./clamav/db/
```

and will be mounted into the container at:

```bash
/var/lib/clamav
```

___

### 🔁 Update Process

When updating the antivirus database:
1. Download the latest `.cvd` files from the official ClamAV database endpoints
2. Replace the existing files in `./clamav/db/`
3. Restart the ClamAV container:
```bash
docker compose restart clamav 
```

### Automated Updates Without FreshClam (Regional Restrictions Friendly)
If direct access to `database.clamav.net` is blocked or unstable (e.g., regional restrictions), you can mirror the databases via GitHub Releases (using a GitHub Actions workflow) and configure your server to download updates from GitHub instead.
On the server, add a cron job that runs once per day:
```bash
cd /path/to/clamav/db

wget -N https://github.com/<USER>/<REPO>/releases/latest/download/main.cvd
wget -N https://github.com/<USER>/<REPO>/releases/latest/download/daily.cvd
wget -N https://github.com/<USER>/<REPO>/releases/latest/download/bytecode.cvd

docker restart natk-clamav
```

___
# 🐇 RabbitMQ — Message Broker for Antivirus

RabbitMQ is used as the backbone for background file scanning.

### Default RabbitMQ services:
| Purpose	              | Component                 |
|-----------------------|---------------------------|
| Antivirus task queue	 | `scan-file` queue         |
| RPC / retries	        | handled by natk-antivirus |
| Monitoring	           | RabbitMQ Web UI           |

### Exposed Ports:
| Port	  | Meaning                              | 
|--------|--------------------------------------|
| 5672   | 	AMQP protocol (microservices queue) | 
| 15672	 | RabbitMQ admin dashboard             | 
___

# 🦠 ClamAV — Antivirus Engine

ClamAV runs in a dedicated container and exposes a TCP scanning port.

### Used by natk-antivirus for:
- Streaming file contents for virus scanning
- Returning virus signatures like: `Win.Trojan.Generic-1234567-0`

___

## 🔌 Default Ports

| Service        | Host Port     | Container Port |
|----------------|---------------|----------------|
| PostgreSQL     | 5432          | 5432           |
| natk-auth      | 8001          | 8080           |
| natk-api       | 8000          | 8080           |
| natk-ai        | 8002          | 8080           |
| natk-pdf       | 8003          | 8080           |
| natk-antivirus | internal only | 8080           |
| MinIO API      | 9000          | 9000           |
| MinIO Console  | 9001          | 9001           |
| RabbitMQ       | 5672 / 15672  | 5672 / 15672   |
| ClamAV         | 3310          | 3310           |

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