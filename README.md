# Hotel Booking System - Microservices Architecture

## Описание проекта

REST API система бронирования отелей на базе Spring Boot с использованием микросервисной архитектуры.

## Содержание

- [Быстрый старт](#быстрый-старт)
- [Архитектура системы](#архитектура-системы)
- [Схемы баз данных](#схемы-баз-данных)
- [Технологический стек](#технологический-стек)
- [Структура проекта](#структура-проекта)
- [Требования](#требования)
- [Запуск приложения](#запуск-приложения)
- [API Endpoints](#api-endpoints)
- [Тестирование API](#тестирование-api)
- [Unit-тестирование и покрытие кода](#unit-тестирование-и-покрытие-кода)
- [Ключевые особенности реализации](#ключевые-особенности-реализации)
- [Тестовые данные](#тестовые-данные)
- [Дополнительные ресурсы](#дополнительные-ресурсы)

## Архитектура системы

Система состоит из следующих компонентов:

1. **Eureka Server** (порт 8761) - Service Discovery
2. **API Gateway** (порт 8080) - маршрутизация запросов
3. **Booking Service** (порт 8081) - управление бронированиями и пользователями
   - База данных: H2 (in-memory) `jdbc:h2:mem:bookingdb`
   - H2 Console: http://localhost:8081/h2-console
4. **Hotel Service** (порт 8082) - управление отелями и номерами
   - База данных: H2 (in-memory) `jdbc:h2:mem:hoteldb`
   - H2 Console: http://localhost:8082/h2-console

**Подключение к БД:** См. раздел "Как подключиться к базам данных" ниже

## Быстрый старт

### 1. Проверьте требования
```bash
java --version   # Требуется Java 17 или 21
mvn --version    # Требуется Maven 3.6+
```

### 2. Запустите все сервисы
```bash
make run         # Сборка и запуск всех микросервисов
```

### 3. Проверьте статус
```bash
make status      # Убедитесь, что все сервисы запущены
```

### 4. Тест API
- **Через Postman:** `Postman_2025-10-25.json`
- **Через curl:** См. раздел "Примеры запросов (curl)" ниже

### 5. Доступ к базам данных
- **Booking Service H2 Console:** http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:bookingdb`, User: `sa`, Password: _(пусто)_
- **Hotel Service H2 Console:** http://localhost:8082/h2-console
  - JDBC URL: `jdbc:h2:mem:hoteldb`, User: `sa`, Password: _(пусто)_

---

## Схемы баз данных

### Booking Service Database (bookingdb)

**JDBC URL:** `jdbc:h2:mem:bookingdb`
**H2 Console:** http://localhost:8081/h2-console
**Username:** `sa` | **Password:** _(пусто)_

#### Таблица: USERS

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| **id** | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор пользователя |
| **username** | VARCHAR(50) | NOT NULL, UNIQUE | Имя пользователя для входа |
| **password** | VARCHAR(255) | NOT NULL | Хэш пароля (BCrypt) |
| **role** | VARCHAR(20) | NOT NULL | Роль: `USER` или `ADMIN` |
| **created_at** | TIMESTAMP | NOT NULL | Дата и время регистрации |

**Пример SQL:**
```sql
-- Создать администратора (пароль: admin123)
INSERT INTO USERS (id, username, password, role, created_at)
VALUES (1, 'admin', '$2a$10$xQvWkzL5K5yHQJz.PqN1Uu5TZGQaJRKk7xF9XqY5vPQZ9YVy.yL5G', 'ADMIN', CURRENT_TIMESTAMP);

-- Просмотр всех пользователей
SELECT id, username, role, created_at FROM USERS;
```

#### Таблица: BOOKINGS

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| **id** | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор бронирования |
| **user_id** | BIGINT | NOT NULL, FK → USERS(id), INDEX | Пользователь, создавший бронирование |
| **room_id** | BIGINT | NOT NULL | ID номера из Hotel Service |
| **start_date** | DATE | NOT NULL | Дата начала бронирования |
| **end_date** | DATE | NOT NULL | Дата окончания бронирования |
| **status** | VARCHAR(20) | NOT NULL | `PENDING`, `CONFIRMED`, `CANCELLED` |
| **request_id** | VARCHAR(100) | UNIQUE, INDEX | UUID для идемпотентности запросов |
| **created_at** | TIMESTAMP | NOT NULL | Дата создания бронирования |
| **updated_at** | TIMESTAMP | NULL | Дата последнего обновления |

**Связи:**
- `BOOKINGS.user_id` → `USERS.id` (Many-to-One)
- `BOOKINGS.room_id` → Ссылка на `ROOMS.id` в Hotel Service (через API)

**Индексы:**
- `idx_user_id` на `user_id` (для быстрого поиска бронирований пользователя)
- `idx_request_id` на `request_id` (для проверки идемпотентности)

**Пример SQL:**
```sql
-- Все бронирования пользователя
SELECT b.*, u.username
FROM BOOKINGS b
JOIN USERS u ON b.user_id = u.id
WHERE u.username = 'john_user'
ORDER BY b.created_at DESC;

-- Статистика по статусам
SELECT status, COUNT(*) as count
FROM BOOKINGS
GROUP BY status;

-- Активные бронирования на определенную дату
SELECT * FROM BOOKINGS
WHERE status = 'CONFIRMED'
  AND start_date <= '2025-11-05'
  AND end_date >= '2025-11-01'
ORDER BY start_date;
```

#### Диаграмма связей (Booking Service)

```
┌─────────────────────┐
│      USERS          │
├─────────────────────┤
│ PK id               │
│    username (UK)    │
│    password         │
│    role             │
│    created_at       │
└─────────────────────┘
          │
          │ 1
          │
          │
          │ N
┌─────────────────────┐
│     BOOKINGS        │
├─────────────────────┤
│ PK id               │
│ FK user_id          │────→ USERS.id
│    room_id          │────→ (Hotel Service ROOMS.id)
│    start_date       │
│    end_date         │
│    status           │
│    request_id (UK)  │
│    created_at       │
│    updated_at       │
└─────────────────────┘
  IDX: user_id, request_id
```

---

### Hotel Service Database (hoteldb)

**JDBC URL:** `jdbc:h2:mem:hoteldb`
**H2 Console:** http://localhost:8082/h2-console
**Username:** `sa` | **Password:** _(пусто)_

#### Таблица: HOTELS

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| **id** | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор отеля |
| **name** | VARCHAR(100) | NOT NULL | Название отеля |
| **address** | VARCHAR(255) | NOT NULL | Адрес отеля |
| **created_at** | TIMESTAMP | NOT NULL | Дата добавления отеля в систему |

**Пример SQL:**
```sql
-- Просмотр всех отелей с количеством номеров
SELECT h.id, h.name, h.address, COUNT(r.id) as room_count
FROM HOTELS h
LEFT JOIN ROOMS r ON h.id = r.hotel_id
GROUP BY h.id, h.name, h.address;
```

#### Таблица: ROOMS

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| **id** | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор номера |
| **hotel_id** | BIGINT | NOT NULL, FK → HOTELS(id), INDEX | Отель, которому принадлежит номер |
| **number** | VARCHAR(20) | NOT NULL | Номер комнаты (101, 202, A1 и т.д.) |
| **available** | BOOLEAN | NOT NULL, DEFAULT TRUE, INDEX | Доступность номера для бронирования |
| **times_booked** | INTEGER | NOT NULL, DEFAULT 0 | Счетчик бронирований (для алгоритма рекомендаций) |
| **created_at** | TIMESTAMP | NOT NULL | Дата добавления номера |

**Связи:**
- `ROOMS.hotel_id` → `HOTELS.id` (Many-to-One)

**Индексы:**
- `idx_hotel_id` на `hotel_id` (для быстрого поиска номеров отеля)
- `idx_available` на `available` (для поиска свободных номеров)

**Пример SQL:**
```sql
-- Номера отеля, отсортированные по загруженности
SELECT r.id, r.number, r.available, r.times_booked
FROM ROOMS r
WHERE r.hotel_id = 1
ORDER BY r.times_booked ASC, r.id ASC;

-- Доступные номера для бронирования
SELECT r.id, h.name as hotel_name, r.number, r.times_booked
FROM ROOMS r
JOIN HOTELS h ON r.hotel_id = h.id
WHERE r.available = TRUE
ORDER BY r.times_booked ASC;
```

#### Таблица: ROOM_RESERVATIONS

| Колонка | Тип | Ограничения | Описание |
|---------|-----|-------------|----------|
| **id** | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Уникальный идентификатор резервации |
| **room_id** | BIGINT | NOT NULL, FK → ROOMS(id), INDEX | Номер, который резервируется |
| **booking_id** | BIGINT | NOT NULL, INDEX | ID бронирования из Booking Service |
| **request_id** | VARCHAR(100) | UNIQUE, INDEX | UUID для идемпотентности |
| **start_date** | DATE | NOT NULL | Дата начала резервации |
| **end_date** | DATE | NOT NULL | Дата окончания резервации |
| **status** | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | `PENDING`, `CONFIRMED`, `RELEASED` |
| **created_at** | TIMESTAMP | NOT NULL | Дата создания резервации |
| **updated_at** | TIMESTAMP | NULL | Дата последнего обновления |

**Связи:**
- `ROOM_RESERVATIONS.room_id` → `ROOMS.id` (Many-to-One)
- `ROOM_RESERVATIONS.booking_id` → Ссылка на `BOOKINGS.id` в Booking Service (через API)

**Индексы:**
- `idx_room_id` на `room_id` (для проверки занятости номера)
- `idx_request_id` на `request_id` (для идемпотентности)
- `idx_booking_id` на `booking_id` (для поиска резерваций по бронированию)

**Пример SQL:**
```sql
-- Проверить занятость номера на даты
SELECT * FROM ROOM_RESERVATIONS
WHERE room_id = 1
  AND status <> 'RELEASED'
  AND start_date <= '2025-11-05'
  AND end_date >= '2025-11-01';

-- Активные резервации с информацией о номере и отеле
SELECT
    rr.id,
    h.name as hotel_name,
    r.number as room_number,
    rr.booking_id,
    rr.start_date,
    rr.end_date,
    rr.status
FROM ROOM_RESERVATIONS rr
JOIN ROOMS r ON rr.room_id = r.id
JOIN HOTELS h ON r.hotel_id = h.id
WHERE rr.status = 'CONFIRMED'
ORDER BY rr.start_date;

-- Статистика бронирований по номерам
SELECT
    r.id,
    r.number,
    r.times_booked,
    COUNT(rr.id) as active_reservations
FROM ROOMS r
LEFT JOIN ROOM_RESERVATIONS rr ON r.id = rr.room_id AND rr.status = 'CONFIRMED'
GROUP BY r.id, r.number, r.times_booked
ORDER BY r.times_booked DESC;
```

#### Диаграмма связей (Hotel Service)

```
┌─────────────────────┐
│      HOTELS         │
├─────────────────────┤
│ PK id               │
│    name             │
│    address          │
│    created_at       │
└─────────────────────┘
          │
          │ 1
          │
          │
          │ N
┌─────────────────────┐
│      ROOMS          │
├─────────────────────┤
│ PK id               │
│ FK hotel_id         │────→ HOTELS.id
│    number           │
│    available        │
│    times_booked     │
│    created_at       │
└─────────────────────┘
  IDX: hotel_id, available
          │
          │ 1
          │
          │
          │ N
┌──────────────────────┐
│ ROOM_RESERVATIONS    │
├──────────────────────┤
│ PK id                │
│ FK room_id           │────→ ROOMS.id
│    booking_id        │────→ (Booking Service BOOKINGS.id)
│    request_id (UK)   │
│    start_date        │
│    end_date          │
│    status            │
│    created_at        │
│    updated_at        │
└──────────────────────┘
  IDX: room_id, request_id, booking_id
```

---

### Связь между микросервисами

```
┌─────────────────────────────────────┐     ┌─────────────────────────────────────┐
│     Booking Service (port 8081)     │     │      Hotel Service (port 8082)      │
│         Database: bookingdb         │     │          Database: hoteldb          │
├─────────────────────────────────────┤     ├─────────────────────────────────────┤
│                                     │     │                                     │
│  ┌─────────┐      ┌──────────┐      │     │  ┌────────┐   ┌───────┐             │
│  │  USERS  │──────│ BOOKINGS │      │     │  │ HOTELS │───│ ROOMS │             │
│  └─────────┘ 1:N  └──────────┘      │     │  └────────┘   └───────┘             │
│                       │             │     │                   │                 │
│                       │ room_id ────┼─────┼───────────────────┼─ id             │
│                       │             │ API │                   │                 │
│                       │ id          │ ←→  │  booking_id       │ 1:N             │
│                       └─────────────┼─────┼───────────────────┼──────┐          │
│                                     │     │                   │      │          │
│                                     │     │          ┌────────▼──────▼──────┐   │
│                                     │     │          │ ROOM_RESERVATIONS    │   │
│                                     │     │          └──────────────────────┘   │
└─────────────────────────────────────┘     └─────────────────────────────────────┘
```

**Ключевые моменты:**
- `BOOKINGS.room_id` хранит ID номера из `ROOMS` таблицы Hotel Service
- `ROOM_RESERVATIONS.booking_id` хранит ID бронирования из `BOOKINGS` таблицы Booking Service
- Связь осуществляется через **Feign Client** API-вызовы между сервисами
- Каждый сервис имеет свою независимую базу данных (Database per Service pattern)

---

### Как подключиться к базам данных

#### H2 Console

1. **Запустите сервисы:**
   ```bash
   make run
   # Или вручную:
   # cd hotel-service && mvn spring-boot:run
   # cd booking-service && mvn spring-boot:run
   ```

2. **Откройте H2 Console в браузере:**

   **Hotel Service:**
   - URL: http://localhost:8082/h2-console
   - JDBC URL: `jdbc:h2:mem:hoteldb`
   - Username: `sa`
   - Password: _(оставьте пустым)_

   **Booking Service:**
   - URL: http://localhost:8081/h2-console
   - JDBC URL: `jdbc:h2:mem:bookingdb`
   - Username: `sa`
   - Password: _(оставьте пустым)_

3. **Нажмите "Connect"** и выполняйте SQL запросы

## Технологический стек

- Java 17+
- Spring Boot 3.4.1
- Spring Cloud 2024.0.0
- Spring Security + JWT
- Spring Data JPA
- H2 Database (in-memory)
- Spring Cloud Eureka
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Lombok
- MapStruct
- SpringDoc OpenAPI

## Структура проекта

```
hotel-booking-system/
├── eureka-server/          # Service Discovery (Eureka Server)
├── api-gateway/            # API Gateway для маршрутизации запросов
├── booking-service/        # Сервис бронирований и управления пользователями
├── hotel-service/          # Сервис управления отелями и номерами
├── .pids/                  # PID файлы запущенных сервисов (для управления процессами)
├── .logs/                  # Логи всех сервисов
├── .idea/                  # Настройки IntelliJ IDEA
├── .git/                   # Git репозиторий
├── pom.xml                 # Родительский Maven POM
├── Makefile                # Автоматизация запуска/остановки сервисов
├── .scripts/               # Вспомогательные скрипты
│   ├── start-service.sh    # Скрипт запуска отдельного сервиса
│   ├── run-tests.sh        # Скрипт запуска всех тестов с покрытием
│   ├── check-java.sh       # Проверка версии Java
│   └── restore-java21.sh   # Скрипт переключения на Java 21
├── docs/                   # Документация
│   └── TEST_DATA.md        # Описание тестовых данных
├── coverage-reports/       # HTML отчеты о покрытии кода
├── .gitignore              # Исключения для Git
├── README.md               # Основная документация
├── hotel-booking-api.json  # OpenAPI спецификация (JSON формат)
└── Postman_2025-10-25.json # Postman коллекция для тестирования API
```

### Описание директорий и файлов

#### Микросервисы

| Директория | Порт | Описание |
|------------|------|----------|
| **eureka-server/** | 8761 | Service Discovery сервер на базе Netflix Eureka. Все микросервисы регистрируются здесь для взаимного обнаружения. |
| **api-gateway/** | 8080 | API Gateway на базе Spring Cloud Gateway. Единая точка входа для всех клиентских запросов, маршрутизирует запросы к соответствующим микросервисам. |
| **booking-service/** | 8081 | Микросервис бронирований. Управляет пользователями (регистрация, JWT аутентификация) и бронированиями (создание, отмена, история). Содержит двухфазную логику бронирования с retry-механизмом. |
| **hotel-service/** | 8082 | Микросервис отелей. Управляет отелями, номерами и резервациями. Реализует алгоритм рекомендаций по загруженности номеров и проверку доступности. |

#### Служебные директории

| Директория | Описание |
|------------|----------|
| **.pids/** | Хранит PID (Process ID) файлы для каждого запущенного сервиса: `eureka.pid`, `hotel.pid`, `booking.pid`, `gateway.pid`. Используется `Makefile` для управления процессами (остановка, проверка статуса). |
| **.logs/** | Хранит логи всех сервисов: `eureka.log`, `hotel.log`, `booking.log`, `gateway.log`. Логи пишутся при запуске через `make start`. Используйте `make logs` для просмотра. |
| **.idea/** | Конфигурационные файлы IntelliJ IDEA (настройки проекта, модули, библиотеки). |
| **.git/** | Git репозиторий с историей версий проекта. |

#### Конфигурационные файлы

| Файл | Описание |
|------|----------|
| **pom.xml** | Родительский Maven POM файл. Определяет общие зависимости, версии библиотек (Spring Boot 3.5.1, Spring Cloud 2024.0.0) и плагины для всех модулей. |
| **.gitignore** | Указывает файлы и директории, игнорируемые Git (target/, .idea/, .pids/, .logs/, и т.д.). |

#### Скрипты автоматизации

| Файл | Описание |
|------|----------|
| **Makefile** | Автоматизация управления сервисами. Команды: `make run` (сборка + запуск), `make start` (запуск в фоне), `make stop` (остановка), `make status` (проверка статуса), `make logs` (просмотр логов), `make clean` (очистка). |
| **.scripts/start-service.sh** | Bash скрипт для запуска одного сервиса в фоновом режиме. Принимает параметры: директория сервиса, лог-файл, PID-файл. Используется `Makefile`. |
| **.scripts/run-tests.sh** | Запускает все unit-тесты для обоих сервисов и генерирует отчеты о покрытии кода в формате HTML. Копирует результаты в `coverage-reports/`. |
| **.scripts/check-java.sh** | Проверяет версию Java. Проект требует Java 17 или 21 (Java 25 не поддерживается). |
| **.scripts/restore-java21.sh** | Скрипт для переключения на Java 21 в Ubuntu/Debian (через update-alternatives). |

#### Документация и инструменты тестирования

| Файл | Описание |
|------|----------|
| **README.md** | Основная документация проекта: архитектура, технологический стек, структура, запуск, тестирование, API endpoints, примеры запросов. |
| **docs/TEST_DATA.md** | Подробное описание тестовых данных, которые автоматически загружаются при запуске микросервисов. |
| **hotel-booking-api.json** | OpenAPI 3.0.3 спецификация всех REST API endpoints в JSON формате. Импортируйте в Swagger Editor, Postman или другие API-клиенты для тестирования. |
| **Postman_2025-10-25.json** | Готовая Postman коллекция с примерами запросов для тестирования всех endpoints системы. Включает примеры для аутентификации, работы с отелями, номерами и бронированиями. |

### Управление проектом с помощью Makefile

Для удобства все операции автоматизированы через Makefile:

```bash
# Показать все доступные команды
make help

# Собрать проект и запустить все сервисы
make run

# Проверить статус сервисов
make status

# Просмотр логов в реальном времени
make logs-booking
make logs-hotel

# Остановить все сервисы
make stop

# Очистить логи и PID файлы
make clean
```

## Требования

Для запуска приложения необходимо:
- **Java 17 или 21** (Java 25 НЕ поддерживается)
- Maven 3.6+
- Операционная система: Linux/macOS/Windows

## Запуск приложения

### Порядок запуска сервисов:

1. Запустить Eureka Server:
```bash
cd eureka-server
mvn spring-boot:run
```

2. Запустить Hotel Service:
```bash
cd hotel-service
mvn spring-boot:run
```

3. Запустить Booking Service:
```bash
cd booking-service
mvn spring-boot:run
```

4. Запустить API Gateway:
```bash
cd api-gateway
mvn spring-boot:run
```

### Альтернативный запуск из корневой директории:

```bash
mvn clean install
# Затем запустить каждый сервис в отдельном терминале
```

## API Endpoints

### Booking Service (через Gateway: http://localhost:8080)

#### Аутентификация (доступно всем):
- `POST /api/user/register` - регистрация пользователя
- `POST /api/user/auth` - авторизация пользователя

#### Управление пользователями (только ADMIN):
- `POST /api/user` - создать пользователя
- `PATCH /api/user` - обновить пользователя
- `DELETE /api/user` - удалить пользователя

#### Бронирования (USER):
- `POST /api/booking` - создать бронирование
- `GET /api/bookings` - получить историю бронирований
- `GET /api/booking/{id}` - получить бронирование по ID
- `DELETE /api/booking/{id}` - отменить бронирование

### Hotel Service (через Gateway: http://localhost:8080)

#### Управление отелями (ADMIN):
- `POST /api/hotels` - добавить отель
- `POST /api/rooms` - добавить номер

#### Просмотр отелей и номеров (USER):
- `GET /api/hotels` - получить список отелей
- `GET /api/rooms` - получить список свободных номеров
- `GET /api/rooms/recommend` - получить рекомендованные номера (отсортированные по timesBooked)

#### Внутренние endpoints (не доступны через Gateway):
- `POST /api/rooms/{id}/confirm-availability` - подтвердить доступность номера
- `POST /api/rooms/{id}/release` - снять бронирование

## Ключевые особенности реализации

### 1. JWT Аутентификация
- Токен действителен 1 час
- Роли: USER и ADMIN
- Каждый сервис проверяет JWT самостоятельно (Resource Server)

### 2. Двухфазное бронирование

Процесс создания бронирования:
1. Booking Service создаёт бронирование в статусе `PENDING`
2. Отправляет запрос в Hotel Service для подтверждения доступности
3. При успехе → статус `CONFIRMED`, при ошибке → `CANCELLED` с компенсацией

### 3. Идемпотентность
- Каждый запрос имеет уникальный `requestId`
- Повторные запросы с тем же `requestId` не создают дубликатов

### 4. Retry-механизм
- Настроена повторная отправка запросов при сбоях
- Экспоненциальная задержка между попытками
- Ограниченное число повторов

### 5. Алгоритм планирования занятости
- Hotel Service ведёт статистику бронирований (`timesBooked`)
- Рекомендованные номера сортируются по возрастанию `timesBooked`
- Обеспечивает равномерную загрузку номеров

### 6. Корреляционные ID
- Сквозная трассировка запросов через все сервисы
- Логирование с `bookingId` для отслеживания процесса бронирования

## Тестовые данные

При запуске микросервисов автоматически загружаются тестовые данные для разработки и тестирования.

**Подробная документация:** См. [docs/TEST_DATA.md](docs/TEST_DATA.md)

### Краткая сводка:

**Hotel Service:**
- 5 отелей: Grand Hotel, Budget Inn, Luxury Resort & Spa, City Center Hotel, Mountain View Lodge
- 52 комнаты (различные номера в каждом отеле)
- 43 доступных комнаты для бронирования

**Booking Service:**
- 6 пользователей (1 ADMIN, 5 USER)
  - admin / admin
  - user1 / password1
  - user2 / password2
  - john / john123
  - alice / alice123
  - bob / bob123
- 8 бронирований с разными статусами (CONFIRMED, PENDING, CANCELLED)

Тестовые данные загружаются автоматически при каждом запуске сервисов через классы `DataLoader`.

## Тестирование API

### Использование Postman коллекции (рекомендуется)

Для удобного тестирования всех endpoints системы используйте готовую Postman коллекцию:

1. Откройте Postman
2. Импортируйте файл `Postman_2025-10-25.json`
3. Коллекция содержит примеры всех запросов с готовыми данными:
   - Регистрация и аутентификация
   - Управление пользователями (ADMIN)
   - Управление отелями и номерами (ADMIN)
   - Получение списка отелей и номеров (USER)
   - Создание и отмена бронирований (USER)

### OpenAPI спецификация

Полная спецификация API доступна в файле `hotel-booking-api.json`. Вы можете:
- Импортировать её в Swagger Editor для визуального просмотра
- Использовать в других API-клиентах (Insomnia, REST Client и др.)
- Генерировать клиентские библиотеки с помощью OpenAPI Generator

## Примеры запросов (curl)

### 1. Регистрация пользователя

```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

### 2. Авторизация

```bash
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

### 3. Создать отель (ADMIN)

```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Hotel",
    "address": "123 Main Street, Moscow"
  }'
```

### 4. Создать номер (ADMIN)

```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelId": 1,
    "number": "101"
  }'
```

### 5. Получить список отелей (USER)

```bash
curl -X GET http://localhost:8080/api/hotels \
  -H "Authorization: Bearer {token}"
```

### 6. Получить рекомендованные номера для Grand Plaza Hotel (USER)

```bash
curl -X GET "http://localhost:8080/api/rooms/recommend?hotelId=1&startDate=2025-11-01&endDate=2025-11-05" \
  -H "Authorization: Bearer {token}"
```

### 7. Создать бронирование с автоподбором номера (USER)

```bash
curl -X POST http://localhost:8080/api/booking \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelId": 1,
    "startDate": "2025-11-01",
    "endDate": "2025-11-05",
    "autoSelect": true
  }'
```

**Примечание:** Используйте ID отелей и номеров из раздела "Тестовые данные" выше.

### 8. Создать бронирование с конкретным номером (USER)

```bash
curl -X POST http://localhost:8080/api/booking \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 9,
    "startDate": "2025-11-01",
    "endDate": "2025-11-05",
    "autoSelect": false
  }'
```

**Примечание:** В этом примере используется номер ID=9 (номер 101 в отеле Sea View Resort).

## Unit-тестирование и покрытие кода

### Запуск тестов

Проект включает комплексные unit-тесты для всех сервисов с измерением покрытия кода через JaCoCo.

#### Быстрый запуск (рекомендуется):

```bash
# Запустить все тесты с генерацией отчетов одной командой
./.scripts/run-tests.sh
```

Скрипт автоматически:
- Запускает все тесты для обоих сервисов
- Генерирует отчеты о покрытии
- Копирует отчеты в `coverage-reports/`
- Показывает результаты

#### Ручной запуск:

```bash
# Из корневой директории проекта
mvn clean test

# Или для отдельных сервисов:
cd booking-service && mvn clean test
cd hotel-service && mvn clean test
```

#### Запуск тестов с генерацией отчета о покрытии:

```bash
# Booking Service
cd booking-service
mvn clean test jacoco:report

# Hotel Service
cd hotel-service
mvn clean test jacoco:report
```

### Просмотр отчетов о покрытии

После запуска тестов с `jacoco:report`, отчеты генерируются в HTML формате:

```bash
# Booking Service
xdg-open booking-service/target/site/jacoco/index.html

# Hotel Service
xdg-open hotel-service/target/site/jacoco/index.html

# Или через браузер:
firefox booking-service/target/site/jacoco/index.html
firefox hotel-service/target/site/jacoco/index.html
```

**Альтернативно**, копии отчетов доступны в:
```
coverage-reports/
├── booking-service/index.html
└── hotel-service/index.html
```

### Что покрыто тестами

**Booking Service:**
- Создание бронирований (успех, неудача, компенсация)
- Автоподбор номеров по алгоритму `timesBooked`
- Идемпотентность запросов
- Валидация дат
- CRUD операции с пользователями
- Аутентификация и JWT

**Hotel Service:**
- CRUD операции с отелями и номерами
- Проверка доступности номеров по датам
- Рекомендации номеров (сортировка по `timesBooked`)
- Создание и освобождение резерваций
- Идемпотентность резерваций

### Дополнительная информация

Отчеты о покрытии кода генерируются автоматически при запуске тестов с помощью JaCoCo и сохраняются в директории `coverage-reports/`. Открыть отчеты можно в браузере по пути `coverage-reports/booking-service/index.html` и `coverage-reports/hotel-service/index.html`.

---

## Дополнительные ресурсы

### Файлы для тестирования
- **Postman_2025-10-25.json** - Готовая коллекция Postman с примерами всех API запросов
- **hotel-booking-api.json** - OpenAPI 3.0.3 спецификация для генерации клиентов и документации

### Документация
- **docs/TEST_DATA.md** - Описание предзагруженных тестовых данных

### Отчеты
- **coverage-reports/** - HTML отчеты о покрытии кода тестами (генерируются при запуске `.scripts/run-tests.sh`)

### Управление сервисами
```bash
make help         # Список всех доступных команд
make run          # Собрать и запустить все сервисы
make status       # Проверить статус сервисов
make logs-booking # Просмотр логов Booking Service
make stop         # Остановить все сервисы
```

### URLs
- **Eureka Dashboard:** http://localhost:8761
- **API Gateway:** http://localhost:8080
- **Booking Service Swagger:** http://localhost:8081/swagger-ui.html
- **Booking Service H2 Console:** http://localhost:8081/h2-console
- **Hotel Service Swagger:** http://localhost:8082/swagger-ui.html
- **Hotel Service H2 Console:** http://localhost:8082/h2-console
