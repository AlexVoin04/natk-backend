FROM python:3.11-slim AS requirements-stage

WORKDIR /tmp

RUN pip install --no-cache-dir poetry

RUN poetry self add poetry-plugin-export

COPY ./pyproject.toml ./poetry.lock* /tmp/

RUN poetry export -f requirements.txt --output requirements.txt --without-hashes

FROM python:3.11-slim

WORKDIR /app

COPY --from=requirements-stage /tmp/requirements.txt /app/requirements.txt

RUN pip install --no-cache-dir --upgrade -r /app/requirements.txt

COPY ./src/main ./src/main

EXPOSE 8080

CMD ["uvicorn", "src.main.app.main:app", "--host", "0.0.0.0", "--port", "8080"]