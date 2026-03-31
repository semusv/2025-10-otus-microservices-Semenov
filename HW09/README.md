# Распределенные транзакции и идемпотентность

## Постановка задачи

### Цель:
В этом ДЗ вы научитесь реализовывать распределенную транзакцию и механизм идемпотентности запросов.

### Сценарий для интернет-магазина:
Реализовать сервисы "Биллинг", "Склад", "Доставка".

Для сервиса "Заказ", в рамках метода "создание заказа" реализовать:
1. Механизм распределенной транзакции (на основе Саги)
2. Механизм идемпотентности запросов

Во время создания заказа необходимо:
1. в сервисе "Биллинг" убедиться, что платеж прошел
2. в сервисе "Склад" зарезервировать конкретный товар на складе
3. в сервисе "Доставка" зарезервировать курьера на конкретный слот времени.

Если хотя бы один из пунктов не получилось сделать, необходимо откатить все остальные изменения.

---

## 0. Описание паттерна для реализации идемпотентности

### Использованный паттерн: **Idempotency Key Pattern** 
#### Принцип работы:
1. Клиент при отправке запроса на создание заказа передает заголовок `X-Idempotency-Key` с уникальным UUID
2. Service проверяет запись с этим key в таблице `request_trackers`:
   - Если запись существует со статусом `PROCESSED` или `PENDING` — возвращает существующий заказ (не создаёт дубликат)
   - Если записи нет — продолжает обработку и сохраняет новый `RequestTracker` со статусом `PENDING`
3. После успешного завершения саги статус обновляется до `PROCESSED`
4. При ошибке статус устанавливается в `FAILED`

#### Состояния RequestTracker:
- `PENDING` — запрос принят и обрабатывается
- `PROCESSED` — запрос успешно обработан (идемпотентный)
- `FAILED` — запрос не удался

#### Архитектура:
```
Client → X-Idempotency-Key → OrderController → OrderService → RequestTrackerRepository
                                                        ↓
                                            saga оркестратор (OrderSagaOrchestrator)
```

#### Основные компоненты:
| Компонент | Назначение |
|-----------|------------|
| `RequestTracker` | Entity для трекинга обработанных запросов |
| `RequestTrackerRepository` | Repository для поиска по idempotency key |
| `OrderServiceImpl.createOrder()` | Логика проверки идемпотентности перед обработкой |

#### Схема таблицы request_trackers:
```sql
CREATE TABLE request_trackers (
    idempotency_key UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, PROCESSED, FAILED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```


## Скрипты:
``` bash

# Важно! 
кафка устанавливается отдельно независимо. Параметры подключения к Кафка сервисам прописаны внутри файла 
properties/env.kafka-connection.yaml
Скорректировать под свое окруженние

# открываем терминал в папке с проектом /HW09/helms  
cd helms




# Быстрая установка всех компонентов
# Обязательно должен стартовать Config-service и Postgres, у остлньых будет 6 попыток на поиск конфигураций
services=(
    auth-service
    user-service
    billing-service
    notification-service
    order-service
    warehouse-service
    delivery-service
    api-gateway
)
for service in postgres config-service ; do
    ./deploy-service.sh install $service HW09
done
sleep 30 # Даем немного времени для постгре и конфиг сервера
for service in "${services[@]}"; do
    ./deploy-service.sh install $service HW09
    sleep 45  # Небольшая пауза между установками
done

# Быстрое обновление всех компонентов
# Обязательно должен стартовать Config-service и Postgres, у остлньых будет 6 попыток на поиск 
services=(
    auth-service
    user-service
    billing-service
    notification-service
    order-service
    warehouse-service
    delivery-service
    api-gateway
)
for service in postgres config-service ; do
    ./deploy-service.sh upgrade $service HW09
done
sleep 30 # Даем немного времени для постгре и конфиг сервера
for service in "${services[@]}"; do
    ./deploy-service.sh upgrade $service HW09
    sleep 45  # Небольшая пауза между установками
done


# Ставим helm через bash скрипт: deploy-service.sh
./deploy-service.sh {action} {service-name} {namespace}
    где action:
        install     - Установить чарт
        upgrade     - Обновить чарт (по умолчанию)
        uninstall   - Удалить релиз
        status      - Показать статус релиза
        list        - Список релизов в namespace
        history     - История релиза
        rollback    - Откатить релиз
        template    - Показать шаблоны
        
# Последовательность установки сервисов
./deploy-service.sh install postgres HW09
./deploy-service.sh install config-service HW09
./deploy-service.sh install auth-service HW09
./deploy-service.sh install user-service HW09
./deploy-service.sh install billing-service HW09
./deploy-service.sh install notification-service HW09
./deploy-service.sh install order-service HW09
./deploy-service.sh install warehouse-service HW09
./deploy-service.sh install delivery-service HW09
./deploy-service.sh install api-gateway HW09



#дожидаемся всех подов. Должно быть 3 пода сервиса, 1 под БД, 1 завершенный JOB 
$ kubectl get po -n HW09
    #kubectl.exe get po -n HW09 
    #NAME                                   READY   STATUS    RESTARTS        AGE
    #HW09-api-gateway-6b5b575679-qsgjd      1/1     Running   6 (3m48s ago)   15h
    #HW09-auth-service-597764d7df-g7vpp     1/1     Running   5 (6m13s ago)   15h
    #HW09-config-service-5f55649d55-hgfk7   1/1     Running   3 (5m56s ago)   15h
    #HW09-postgresql-0                      1/1     Running   2 (11m ago)     8d
    #HW09-user-service-55c66746db-l8zcj     1/1     Running   5 (6m29s ago)   15

#Запускаем тесты
$ newman run ../postman/otus-HW09.postman_collection.json
 
  
#Удаляем неймспейс
$ kubectl delete ns HW09

```