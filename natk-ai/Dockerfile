FROM python:3.11-slim

WORKDIR /src/main

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ./src/main ./
COPY .env.example ./
# переменный окружения задаются на запуске контейнера

CMD ["python", "app/main.py"]