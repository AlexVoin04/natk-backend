# 🧩 NATK Backend

This repository contains a microservices architecture with:

- **natk-api** — Main application API
- **natk-auth** — Authentication and JWT handling service
- **PostgreSQL** — Database for persistence

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

- Build Docker images for `natk-auth` and `natk-api`
- Start the `natk-auth`, `natk-api`, and `db` containers
- Automatically create a shared network `natk-net`

---

### 🛠 Option 2: Manually Build Docker Images
Build images manually:

🔐 Build natk-auth image
```shell
docker build -t natk-auth:0.0.1 ./natk-auth
```

🌐 Build natk-api image
```shell
docker build -t natk-api:0.0.1 ./natk-api
```

▶️ Run the containers
```shell
docker-compose -f docker-compose.yml up
```

---

## 🔌 Default Ports

| Service     | Host Port | Container Port |
|-------------|-----------|----------------|
| PostgreSQL  | 5432      | 5432           |
| natk-auth   | 8001      | 8080           |
| natk-api    | 8000      | 8080           |

---

## 📣 After Running

Once started, you should see:

- PostgreSQL initialized and ready
- `natk-auth` running at http://localhost:8001
- `natk-api` running at http://localhost:8000

You can now interact with the services via their respective endpoints.

---