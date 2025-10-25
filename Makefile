.PHONY: help build start stop restart status logs clean install run run-all

# Цвета для вывода
GREEN=\033[0;32m
YELLOW=\033[1;33m
RED=\033[0;31m
NC=\033[0m # No Color

# Директории сервисов
EUREKA_DIR=eureka-server
GATEWAY_DIR=api-gateway
BOOKING_DIR=booking-service
HOTEL_DIR=hotel-service

# PID файлы
PID_DIR=.pids
EUREKA_PID=$(PID_DIR)/eureka.pid
GATEWAY_PID=$(PID_DIR)/gateway.pid
BOOKING_PID=$(PID_DIR)/booking.pid
HOTEL_PID=$(PID_DIR)/hotel.pid

# Лог файлы
LOG_DIR=.logs
EUREKA_LOG=$(LOG_DIR)/eureka.log
GATEWAY_LOG=$(LOG_DIR)/gateway.log
BOOKING_LOG=$(LOG_DIR)/booking.log
HOTEL_LOG=$(LOG_DIR)/hotel.log

help:
	@echo "$(GREEN)╔══════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(GREEN)║      Hotel Booking System - Управление сервисами         ║$(NC)"
	@echo "$(GREEN)╚══════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(YELLOW)Доступные команды:$(NC)"
	@echo "  $(GREEN)make install$(NC)     - Установить зависимости и собрать проект"
	@echo "  $(GREEN)make build$(NC)       - Собрать все модули (mvn clean install)"
	@echo "  $(GREEN)make run$(NC)         - Собрать и запустить все сервисы (build + start)"
	@echo "  $(GREEN)make start$(NC)       - Запустить все сервисы в фоне"
	@echo "  $(GREEN)make stop$(NC)        - Остановить все сервисы"
	@echo "  $(GREEN)make restart$(NC)     - Перезапустить все сервисы"
	@echo "  $(GREEN)make status$(NC)      - Показать статус всех сервисов"
	@echo "  $(GREEN)make logs$(NC)        - Показать логи всех сервисов"
	@echo "  $(GREEN)make logs-eureka$(NC) - Показать логи Eureka Server"
	@echo "  $(GREEN)make logs-gateway$(NC)- Показать логи API Gateway"
	@echo "  $(GREEN)make logs-booking$(NC)- Показать логи Booking Service"
	@echo "  $(GREEN)make logs-hotel$(NC)  - Показать логи Hotel Service"
	@echo "  $(GREEN)make clean$(NC)       - Очистить логи и PID файлы"
	@echo "  $(GREEN)make clean-all$(NC)   - Очистить всё (включая target)"
	@echo ""
	@echo "$(YELLOW)Примеры:$(NC)"
	@echo "  make run                      # Собрать и запустить всё"
	@echo "  make status                   # Проверить статус"
	@echo "  make logs-booking             # Смотреть логи Booking Service"
	@echo "  make stop                     # Остановить всё"

install:
	@echo "$(GREEN)Проверка Java версии...$(NC)"
	@./.scripts/check-java.sh || exit 1
	@echo ""
	@echo "$(GREEN)Проверка Maven...$(NC)"
	@which mvn > /dev/null || (echo "$(RED)Maven не установлен! Установите: sudo apt install maven$(NC)" && exit 1)
	@echo "$(GREEN)Maven найден:$(NC) $$(mvn --version | head -1)"
	@echo ""
	@echo "$(GREEN)Сборка проекта...$(NC)"
	@$(MAKE) build

build:
	@echo "$(GREEN)Сборка всех модулей...$(NC)"
	@mvn clean install -DskipTests
	@echo "$(GREEN)✓ Сборка завершена$(NC)"

run: build start
	@echo ""
	@echo "$(GREEN)╔══════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(GREEN)║  ✓ Проект собран и все сервисы успешно запущены!        ║$(NC)"
	@echo "$(GREEN)╚══════════════════════════════════════════════════════════╝$(NC)"
	@echo ""

run-all: run

$(PID_DIR):
	@mkdir -p $(PID_DIR)

$(LOG_DIR):
	@mkdir -p $(LOG_DIR)

start: $(PID_DIR) $(LOG_DIR)
	@echo "$(GREEN)╔══════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(GREEN)║            Запуск микросервисов...                       ║$(NC)"
	@echo "$(GREEN)╚══════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@$(MAKE) start-eureka
	@echo "$(YELLOW)Ожидание запуска Eureka Server (30 сек)...$(NC)"
	@sleep 30
	@$(MAKE) start-hotel
	@echo "$(YELLOW)Ожидание запуска Hotel Service (15 сек)...$(NC)"
	@sleep 15
	@$(MAKE) start-booking
	@echo "$(YELLOW)Ожидание запуска Booking Service (15 сек)...$(NC)"
	@sleep 15
	@$(MAKE) start-gateway
	@echo ""
	@echo "$(GREEN)✓ Все сервисы запущены!$(NC)"
	@echo ""
	@$(MAKE) status

start-eureka: $(PID_DIR) $(LOG_DIR)
	@if [ -f $(EUREKA_PID) ] && kill -0 $$(cat $(EUREKA_PID)) 2>/dev/null; then \
		echo "$(YELLOW)⚠ Eureka Server уже запущен (PID: $$(cat $(EUREKA_PID)))$(NC)"; \
	else \
		echo "$(GREEN)→ Запуск Eureka Server...$(NC)"; \
		./.scripts/start-service.sh $(EUREKA_DIR) $(EUREKA_LOG) $(EUREKA_PID) > /dev/null; \
		sleep 1; \
		echo "$(GREEN)  ✓ Eureka Server запущен (PID: $$(cat $(EUREKA_PID)))$(NC)"; \
		echo "$(GREEN)   URL: http://localhost:8761$(NC)"; \
	fi

start-gateway: $(PID_DIR) $(LOG_DIR)
	@if [ -f $(GATEWAY_PID) ] && kill -0 $$(cat $(GATEWAY_PID)) 2>/dev/null; then \
		echo "$(YELLOW)⚠ API Gateway уже запущен (PID: $$(cat $(GATEWAY_PID)))$(NC)"; \
	else \
		echo "$(GREEN)→ Запуск API Gateway...$(NC)"; \
		./.scripts/start-service.sh $(GATEWAY_DIR) $(GATEWAY_LOG) $(GATEWAY_PID) > /dev/null; \
		sleep 1; \
		echo "$(GREEN)  ✓ API Gateway запущен (PID: $$(cat $(GATEWAY_PID)))$(NC)"; \
		echo "$(GREEN)   URL: http://localhost:8080$(NC)"; \
	fi

start-booking: $(PID_DIR) $(LOG_DIR)
	@if [ -f $(BOOKING_PID) ] && kill -0 $$(cat $(BOOKING_PID)) 2>/dev/null; then \
		echo "$(YELLOW)⚠ Booking Service уже запущен (PID: $$(cat $(BOOKING_PID)))$(NC)"; \
	else \
		echo "$(GREEN)→ Запуск Booking Service...$(NC)"; \
		./.scripts/start-service.sh $(BOOKING_DIR) $(BOOKING_LOG) $(BOOKING_PID) > /dev/null; \
		sleep 1; \
		echo "$(GREEN)  ✓ Booking Service запущен (PID: $$(cat $(BOOKING_PID)))$(NC)"; \
		echo "$(GREEN)  URL: http://localhost:8081$(NC)"; \
		echo "$(GREEN)  Swagger: http://localhost:8081/swagger-ui.html$(NC)"; \
	fi

start-hotel: $(PID_DIR) $(LOG_DIR)
	@if [ -f $(HOTEL_PID) ] && kill -0 $$(cat $(HOTEL_PID)) 2>/dev/null; then \
		echo "$(YELLOW)⚠ Hotel Service уже запущен (PID: $$(cat $(HOTEL_PID)))$(NC)"; \
	else \
		echo "$(GREEN)→ Запуск Hotel Service...$(NC)"; \
		./.scripts/start-service.sh $(HOTEL_DIR) $(HOTEL_LOG) $(HOTEL_PID) > /dev/null; \
		sleep 1; \
		echo "$(GREEN)  ✓ Hotel Service запущен (PID: $$(cat $(HOTEL_PID)))$(NC)"; \
		echo "$(GREEN)  URL: http://localhost:8082$(NC)"; \
		echo "$(GREEN)  Swagger: http://localhost:8082/swagger-ui.html$(NC)"; \
	fi

stop:
	@echo "$(YELLOW)Остановка всех сервисов...$(NC)"
	@$(MAKE) stop-gateway
	@$(MAKE) stop-booking
	@$(MAKE) stop-hotel
	@$(MAKE) stop-eureka
	@echo "$(GREEN)✓ Все сервисы остановлены$(NC)"

stop-eureka:
	@if [ -f $(EUREKA_PID) ]; then \
		if kill -0 $$(cat $(EUREKA_PID)) 2>/dev/null; then \
			echo "$(YELLOW)→ Остановка Eureka Server (PID: $$(cat $(EUREKA_PID)))...$(NC)"; \
			kill $$(cat $(EUREKA_PID)) 2>/dev/null || true; \
			sleep 2; \
			kill -9 $$(cat $(EUREKA_PID)) 2>/dev/null || true; \
			echo "$(GREEN)  ✓ Eureka Server остановлен$(NC)"; \
		fi; \
		rm -f $(EUREKA_PID); \
	fi

stop-gateway:
	@if [ -f $(GATEWAY_PID) ]; then \
		if kill -0 $$(cat $(GATEWAY_PID)) 2>/dev/null; then \
			echo "$(YELLOW)→ Остановка API Gateway (PID: $$(cat $(GATEWAY_PID)))...$(NC)"; \
			kill $$(cat $(GATEWAY_PID)) 2>/dev/null || true; \
			sleep 2; \
			kill -9 $$(cat $(GATEWAY_PID)) 2>/dev/null || true; \
			echo "$(GREEN)  ✓ API Gateway остановлен$(NC)"; \
		fi; \
		rm -f $(GATEWAY_PID); \
	fi

stop-booking:
	@if [ -f $(BOOKING_PID) ]; then \
		if kill -0 $$(cat $(BOOKING_PID)) 2>/dev/null; then \
			echo "$(YELLOW)→ Остановка Booking Service (PID: $$(cat $(BOOKING_PID)))...$(NC)"; \
			kill $$(cat $(BOOKING_PID)) 2>/dev/null || true; \
			sleep 2; \
			kill -9 $$(cat $(BOOKING_PID)) 2>/dev/null || true; \
			echo "$(GREEN)  ✓ Booking Service остановлен$(NC)"; \
		fi; \
		rm -f $(BOOKING_PID); \
	fi

stop-hotel:
	@if [ -f $(HOTEL_PID) ]; then \
		if kill -0 $$(cat $(HOTEL_PID)) 2>/dev/null; then \
			echo "$(YELLOW)→ Остановка Hotel Service (PID: $$(cat $(HOTEL_PID)))...$(NC)"; \
			kill $$(cat $(HOTEL_PID)) 2>/dev/null || true; \
			sleep 2; \
			kill -9 $$(cat $(HOTEL_PID)) 2>/dev/null || true; \
			echo "$(GREEN)  ✓ Hotel Service остановлен$(NC)"; \
		fi; \
		rm -f $(HOTEL_PID); \
	fi

restart: stop
	@echo "$(YELLOW)Перезапуск через 5 секунд...$(NC)"
	@sleep 5
	@$(MAKE) start

status:
	@echo "$(GREEN)╔══════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(GREEN)║              Статус микросервисов                        ║$(NC)"
	@echo "$(GREEN)╚══════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(YELLOW)Сервис              PID      Статус        Порт   URL$(NC)"
	@echo "────────────────────────────────────────────────────────────"
	@if [ -f $(EUREKA_PID) ] && kill -0 $$(cat $(EUREKA_PID)) 2>/dev/null; then \
		echo "$(GREEN)Eureka Server       $$(cat $(EUREKA_PID))     ✓ Работает    8761   http://localhost:8761$(NC)"; \
	else \
		echo "$(RED)Eureka Server       -        ✗ Остановлен  8761   -$(NC)"; \
	fi
	@if [ -f $(HOTEL_PID) ] && kill -0 $$(cat $(HOTEL_PID)) 2>/dev/null; then \
		echo "$(GREEN)Hotel Service       $$(cat $(HOTEL_PID))     ✓ Работает    8082   http://localhost:8082$(NC)"; \
	else \
		echo "$(RED)Hotel Service       -        ✗ Остановлен  8082   -$(NC)"; \
	fi
	@if [ -f $(BOOKING_PID) ] && kill -0 $$(cat $(BOOKING_PID)) 2>/dev/null; then \
		echo "$(GREEN)Booking Service     $$(cat $(BOOKING_PID))     ✓ Работает    8081   http://localhost:8081$(NC)"; \
	else \
		echo "$(RED)Booking Service     -        ✗ Остановлен  8081   -$(NC)"; \
	fi
	@if [ -f $(GATEWAY_PID) ] && kill -0 $$(cat $(GATEWAY_PID)) 2>/dev/null; then \
		echo "$(GREEN)API Gateway         $$(cat $(GATEWAY_PID))     ✓ Работает    8080   http://localhost:8080$(NC)"; \
	else \
		echo "$(RED)API Gateway         -        ✗ Остановлен  8080   -$(NC)"; \
	fi
	@echo ""
	@echo "$(YELLOW)Swagger UI:$(NC)"
	@echo "  Booking: http://localhost:8081/swagger-ui.html"
	@echo "  Hotel:   http://localhost:8082/swagger-ui.html"

logs:
	@echo "$(GREEN)Последние логи всех сервисов:$(NC)"
	@echo ""
	@echo "$(YELLOW)═══ Eureka Server ═══$(NC)"
	@tail -n 20 $(EUREKA_LOG) 2>/dev/null || echo "$(RED)Логи не найдены$(NC)"
	@echo ""
	@echo "$(YELLOW)═══ Hotel Service ═══$(NC)"
	@tail -n 20 $(HOTEL_LOG) 2>/dev/null || echo "$(RED)Логи не найдены$(NC)"
	@echo ""
	@echo "$(YELLOW)═══ Booking Service ═══$(NC)"
	@tail -n 20 $(BOOKING_LOG) 2>/dev/null || echo "$(RED)Логи не найдены$(NC)"
	@echo ""
	@echo "$(YELLOW)═══ API Gateway ═══$(NC)"
	@tail -n 20 $(GATEWAY_LOG) 2>/dev/null || echo "$(RED)Логи не найдены$(NC)"

logs-eureka:
	@echo "$(GREEN)Логи Eureka Server (Ctrl+C для выхода):$(NC)"
	@tail -f $(EUREKA_LOG) 2>/dev/null || echo "$(RED)Логи не найдены. Запустите сервис командой: make start-eureka$(NC)"

logs-gateway:
	@echo "$(GREEN)Логи API Gateway (Ctrl+C для выхода):$(NC)"
	@tail -f $(GATEWAY_LOG) 2>/dev/null || echo "$(RED)Логи не найдены. Запустите сервис командой: make start-gateway$(NC)"

logs-booking:
	@echo "$(GREEN)Логи Booking Service (Ctrl+C для выхода):$(NC)"
	@tail -f $(BOOKING_LOG) 2>/dev/null || echo "$(RED)Логи не найдены. Запустите сервис командой: make start-booking$(NC)"

logs-hotel:
	@echo "$(GREEN)Логи Hotel Service (Ctrl+C для выхода):$(NC)"
	@tail -f $(HOTEL_LOG) 2>/dev/null || echo "$(RED)Логи не найдены. Запустите сервис командой: make start-hotel$(NC)"

clean:
	@echo "$(YELLOW)Очистка логов и PID файлов...$(NC)"
	@rm -rf $(PID_DIR) $(LOG_DIR)
	@echo "$(GREEN)✓ Очистка завершена$(NC)"

clean-all: clean
	@echo "$(YELLOW)Очистка Maven target директорий...$(NC)"
	@mvn clean
	@echo "$(GREEN)✓ Полная очистка завершена$(NC)"
