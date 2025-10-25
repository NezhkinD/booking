#!/bin/bash

# Скрипт для запуска микросервисов
# Использование: ./start-service.sh <service-dir> <log-file> <pid-file>

SERVICE_DIR=$1
LOG_FILE=$2
PID_FILE=$3

# Создать директории если не существуют
mkdir -p .pids .logs

# Запустить сервис
cd "$SERVICE_DIR" || exit 1
nohup mvn spring-boot:run > "../$LOG_FILE" 2>&1 &
PID=$!
echo $PID > "../$PID_FILE"
echo $PID
