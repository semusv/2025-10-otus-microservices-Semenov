#!/bin/bash

# === Универсальный Helm-скрипт для развёртывания сервисов ===
#
# Использование:
#   ./deploy-service.sh [ACTION] [SERVICE_NAME] [NAMESPACE]
#
# Примеры:
#   ./deploy-service.sh install api-gateway hw09
#   ./deploy-service.sh upgrade user-service hw09
#   ./deploy-service.sh uninstall auth-service hw09
#
# Все чарты ожидаются в ./hw09.<service-name>
# Секреты — в ./secrets/secret-<namespace>.yaml


# === Значения по умолчанию ===
DEFAULT_ACTION="upgrade"
DEFAULT_NAMESPACE="default"

# === Параметры из командной строки ===
ACTION="${1:-$DEFAULT_ACTION}"
SERVICE_NAME="${2:-}"
NAMESPACE="${3:-$DEFAULT_NAMESPACE}"

# === Пути ===
CHART_PATH="./hw09-$SERVICE_NAME"
SECRETS_FILE="./secrets/secret-pg.yaml"
SERVICES_FILE="./properties/services.yaml"
ENV_SERVICES_FILE="./properties/env.services.yaml"
DB_CONN_FILE="./properties/db-connection.yaml"
ENV_DB_CONN_FILE="./properties/env.db-connection.yaml"
ENV_KAFKA_CONN_FILE="./properties/env.kafka-connection.yaml"

# === Проверка обязательных параметров ===
if [[ -z "$SERVICE_NAME" ]]; then
  echo "❌ Ошибка: Не указано имя сервиса."
  echo "Использование: $0 [action] <service-name> [namespace]"
  echo "Пример: $0 upgrade api-gateway hw09"
  exit 1
fi

# Проверим, существует ли чарт
if [[ ! -d "$CHART_PATH" ]]; then
  echo "❌ Ошибка: Директория чарта не найдена: $CHART_PATH"
  echo "Убедитесь, что чарт называется 'hw09-$SERVICE_NAME'"
  exit 1
fi

# Если секретов нет — попробуем использовать общий или пропустим
if [[ ! -f "$SECRETS_FILE" ]]; then
  echo "⚠️  Файл секретов не найден: $SECRETS_FILE"
  echo "    Продолжаем без --values. Убедитесь, что значения в чарте по умолчанию."
  SECRETS_FLAG=""
else
  SECRETS_FLAG="--values $SECRETS_FILE"
fi

# Найдем файлы с параметрами сервисов
if [[ ! -f "$SERVICES_FILE" ]]; then
  echo "⚠️  Файл параметров сервисов не найден: $SERVICES_FILE"
  echo "    Продолжаем без --values. Убедитесь, что значения в чарте по умолчанию."
  SERVICES_FLAG=""
else
  SERVICES_FLAG="--values $SERVICES_FILE"
fi


# Найдем файлы с параметрами окружения сервисов
if [[ ! -f "$ENV_SERVICES_FILE" ]]; then
  echo "⚠️  Файл параметров сервисов не найден: $ENV_SERVICES_FILE"
  echo "    Продолжаем без --values. Убедитесь, что значения в чарте по умолчанию."
  ENV_SERVICES_FLAG=""
else
  ENV_SERVICES_FLAG="--values $ENV_SERVICES_FILE"
fi


# Найдем файлы с параметрами окружения подключения к БД
if [[ ! -f "$ENV_DB_CONN_FILE" ]]; then
  echo "⚠️  Файл параметров БД не найден: $ENV_DB_CONN_FILE"
  echo "    Продолжаем без --values. Убедитесь, что значения в чарте по умолчанию."
  ENV_DB_CONN_FLAG=""
else
  ENV_DB_CONN_FLAG="--values $ENV_DB_CONN_FILE"
fi

# Найдем файлы с параметрами подключения к БД
if [[ ! -f "$DB_CONN_FILE" ]]; then
  echo "⚠️  Файл параметров БД не найден: $DB_CONN_FILE"
  echo "    Продолжаем без --values. Убедитесь, что значения в чарте по умолчанию."
  DB_CONN_FLAG=""
else
  DB_CONN_FLAG="--values $DB_CONN_FILE"
fi


# Найдем файлы с параметрами окружения подключения к Kafka
if [[ ! -f "$ENV_KAFKA_CONN_FILE" ]]; then
  echo "⚠️  Файл параметров БД не найден: $ENV_KAFKA_CONN_FILE"
  echo "    Продолжаем без --values. Убедитесь, что значения в чарте по умолчанию."
  ENV_KAFKA_CONN_FLAG=""
else
  ENV_KAFKA_CONN_FLAG="--values $ENV_KAFKA_CONN_FILE"
fi

RELEASE_NAME="hw09-$SERVICE_NAME"
REVISION="${4:-1}"  # для rollback

# === Создаём namespace, если нужно ===
kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || {
  echo "🔧 Создаём namespace: $NAMESPACE"
  kubectl create namespace "$NAMESPACE"
}

echo "🚀 Выполняем Helm $ACTION для сервиса '$SERVICE_NAME' (релиз: $RELEASE_NAME) в namespace '$NAMESPACE' из chart-path '$CHART_PATH' "

# === Основной switch по действиям ===
case $ACTION in
  "install")
    helm install "$RELEASE_NAME" \
      "$CHART_PATH" \
      --namespace "$NAMESPACE" \
      --dependency-update \
      --create-namespace \
      $SECRETS_FLAG \
      $SERVICES_FLAG \
      $ENV_SERVICES_FLAG \
      $DB_CONN_FLAG \
      $ENV_DB_CONN_FLAG \
      $ENV_KAFKA_CONN_FLAG \
      --render-subchart-notes
    ;;

  "upgrade")
    helm upgrade "$RELEASE_NAME" \
      "$CHART_PATH" \
      --namespace "$NAMESPACE" \
      --dependency-update \
      --create-namespace \
      $SECRETS_FLAG \
      $SERVICES_FLAG \
      $ENV_SERVICES_FLAG \
      $DB_CONN_FLAG \
      $ENV_DB_CONN_FLAG \
      $ENV_KAFKA_CONN_FLAG \
      --render-subchart-notes \
      --install
    ;;

  "uninstall")
    echo "⚠️  ВНИМАНИЕ: Будет удалён релиз '$RELEASE_NAME' из namespace '$NAMESPACE'"
    read -p "Вы уверены? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
      helm uninstall "$RELEASE_NAME" --namespace "$NAMESPACE"
      echo "✅ Релиз '$RELEASE_NAME' удалён."
    else
      echo "❌ Удаление отменено."
      exit 0
    fi
    ;;

  "status")
    helm status "$RELEASE_NAME" --namespace "$NAMESPACE"
    ;;

  "list")
    helm list --namespace "$NAMESPACE"
    ;;

  "history")
    helm history "$RELEASE_NAME" --namespace "$NAMESPACE"
    ;;

  "rollback")
    echo "⏪ Откатываем релиз '$RELEASE_NAME' к ревизии $REVISION"
    helm rollback "$RELEASE_NAME" "$REVISION" --namespace "$NAMESPACE"
    ;;

  "template")
    echo "📄 Рендерим шаблоны для релиза '$RELEASE_NAME'..."
    helm template "$RELEASE_NAME" \
      "$CHART_PATH" \
      --namespace "$NAMESPACE" \
      $SECRETS_FLAG \
      $ENV_SERVICES_FLAG \
      $SERVICES_FLAG \
      $DB_CONN_FLAG \
      $ENV_DB_CONN_FLAG \
      $ENV_KAFKA_CONN_FLAG \
      --render-subchart-notes
    ;;

  *)
    echo "❌ Неизвестное действие: $ACTION"
    echo "Доступные действия:"
    echo "  install     - Установить чарт"
    echo "  upgrade     - Обновить (по умолчанию)"
    echo "  uninstall   - Удалить релиз"
    echo "  status      - Статус релиза"
    echo "  list        - Список релизов в namespace"
    echo "  history     - История релиза"
    echo "  rollback    - Откатить (указать ревизию)"
    echo "  template    - Рендер шаблонов"
    exit 1
    ;;
esac

# === Проверка результата ===
if [[ $? -eq 0 ]]; then
  if [[ "$ACTION" != "template" ]]; then
    echo "✅ Helm $ACTION успешно завершён для '$SERVICE_NAME' в namespace '$NAMESPACE'"
  fi
else
  echo "❌ Helm $ACTION завершился с ошибкой"
  exit 1
fi