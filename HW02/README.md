# Домашнее задание - Основы работы с Docker 
## Приложение в docker-образ и запушить его на Dockerhub

### Цель:
В этом ДЗ вы сможете обернуть приложение в docker-образ и запушить его на Dockerhub.

### Описание/Пошаговая инструкция выполнения домашнего задания:
В этом и последующих ДЗ у вас будет два варианта задания на выбор:

### Вариант 1 (С КОДОМ)

- Шаг 1. Создать минимальный сервис, который
  - отвечает на порту 8000
  - имеет http-метод:
  - GET /health/
  - RESPONSE: {"status": "OK"}


- Шаг 2. Cобрать локально образ приложения в докер контейнер под архитектуру AMD64.
  - Запушить образ в dockerhub

### На выходе необходимо предоставить
- имя репозитория и тэг на Dockerhub
- ссылку на github c Dockerfile, либо приложить Dockerfile в ДЗ

### Краткая инструкция скомандами
```bash
# Сборка
docker build -t vvsem/simple-service:latest -f simple-service/Dockerfile .
docker build --platform linux/amd64 -t vvsem/simple-service:latest -f simple-service/Dockerfile .

# Запускаем контейнер для теста
docker run -d -p 8000:8000 --name simple-service-test vvsem/simple-service:latest

# Проверяем логи
docker logs simple-service-test

# Проверяем, что приложение отвечает
curl http://localhost:8000
curl http://localhost:8000/health

# Останавливаем тестовый контейнер
docker stop simple-service-test
docker rm simple-service-test

# Добавить дополнительный тег
docker tag vvsem/simple-service:latest vvsem/simple-service:1.0.0

# Отправить все теги
docker push vvsem/simple-service:1.0.0
docker push vvsem/simple-service:latest
```

