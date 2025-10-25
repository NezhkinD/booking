# Тестовые данные

Этот документ описывает тестовые данные, которые автоматически загружаются при запуске микросервисов.

## Обзор

Тестовые данные загружаются автоматически при старте каждого сервиса с помощью классов `DataLoader`, реализующих `CommandLineRunner`. Данные загружаются в in-memory базы данных H2.

## Hotel Service

**Расположение:** `hotel-service/src/main/java/com/booking/hotel/config/DataLoader.java`

### Отели

Создано 5 отелей:

| ID | Название | Адрес | Количество комнат |
|----|----------|-------|-------------------|
| 1 | Grand Hotel | 123 Main Street, New York, NY 10001 | 10 |
| 2 | Budget Inn | 456 Oak Avenue, Los Angeles, CA 90001 | 7 |
| 3 | Luxury Resort & Spa | 789 Beach Road, Miami, FL 33101 | 15 |
| 4 | City Center Hotel | 321 Downtown Street, Chicago, IL 60601 | 8 |
| 5 | Mountain View Lodge | 555 Alpine Road, Denver, CO 80201 | 12 |

### Комнаты

Всего создано **52 комнаты** в разных отелях:

#### Grand Hotel (ID: 1)
- Комнаты: 101-110
- Доступные: 101-108 (8 комнат)
- Недоступные: 109-110 (забронированы 5 раз)

#### Budget Inn (ID: 2)
- Комнаты: 201-207
- Все комнаты доступны (7 комнат)

#### Luxury Resort & Spa (ID: 3)
- Комнаты: 301-315
- Доступные: 301-312 (12 комнат)
- Недоступные: 313-315 (забронированы 3 раза)

#### City Center Hotel (ID: 4)
- Комнаты: 401-408
- Доступные: 401-406 (6 комнат)
- Недоступные: 407-408 (забронированы 2 раза)

#### Mountain View Lodge (ID: 5)
- Комнаты: 501-512
- Доступные: 501-510 (10 комнат)
- Недоступные: 511-512 (забронированы 4 раза)

## Booking Service

**Расположение:** `booking-service/src/main/java/com/booking/service/config/DataLoader.java`

### Пользователи

Создано 6 пользователей с хешированными паролями (BCrypt):

| ID | Username | Пароль | Роль |
|----|----------|---------|------|
| 1 | admin | admin | ADMIN |
| 2 | user1 | password1 | USER |
| 3 | user2 | password2 | USER |
| 4 | john | john123 | USER |
| 5 | alice | alice123 | USER |
| 6 | bob | bob123 | USER |

### Бронирования

Создано 8 бронирований с разными статусами:

| ID | Пользователь | Комната | Даты | Статус |
|----|--------------|---------|------|---------|
| 1 | user1 | 1 | +5 до +7 дней | CONFIRMED |
| 2 | john | 3 | +10 до +15 дней | CONFIRMED |
| 3 | alice | 5 | +3 до +4 дней | PENDING |
| 4 | user2 | 7 | +2 до +5 дней | CANCELLED |
| 5 | bob | 10 | +20 до +25 дней | CONFIRMED |
| 6 | john | 15 | +30 до +35 дней | CONFIRMED |
| 7 | alice | 20 | +7 до +10 дней | PENDING |
| 8 | user1 | 2 | -10 до -7 дней (прошлое) | CONFIRMED |

**Примечание:** Даты указаны относительно текущей даты запуска приложения.

### Статистика бронирований

- **CONFIRMED:** 5 бронирований
- **PENDING:** 2 бронирования
- **CANCELLED:** 1 бронирование

## Использование

### Запуск с тестовыми данными

Тестовые данные загружаются автоматически при каждом запуске сервисов:

```bash
# Запустить все сервисы
make start-all

# Или запустить отдельные сервисы
make start-eureka
make start-hotel
make start-booking
make start-gateway
```

### Проверка загрузки данных

После запуска сервисов вы увидите в логах:

**Hotel Service:**
```
Loading test data for hotel-service...
Created 5 hotels
Created 52 rooms across all hotels
Test data loading completed successfully!
Available hotels: 5
Total rooms: 52
Available rooms: 43
```

**Booking Service:**
```
Loading test data for booking-service...
Created 6 users
Created 8 bookings
Test data loading completed successfully!
Total users: 6
Total bookings: 8
Confirmed bookings: 5
Pending bookings: 2
Cancelled bookings: 1

========== TEST USERS ==========
Admin: username=admin, password=admin
User1: username=user1, password=password1
User2: username=user2, password=password2
John: username=john, password=john123
Alice: username=alice, password=alice123
Bob: username=bob, password=bob123
================================
```

### Доступ к H2 Console

Вы можете просмотреть данные через H2 Console:

**Hotel Service:**
- URL: http://localhost:8082/h2-console
- JDBC URL: `jdbc:h2:mem:hoteldb`
- Username: `sa`
- Password: (оставить пустым)

**Booking Service:**
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:bookingdb`
- Username: `sa`
- Password: (оставить пустым)

### Тестирование API с тестовыми данными

#### Аутентификация

```bash
# Войти как обычный пользователь
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password1"
  }'

# Войти как администратор
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

#### Просмотр отелей

```bash
# Получить все отели
curl http://localhost:8080/api/hotels

# Получить отель по ID
curl http://localhost:8080/api/hotels/1
```

#### Просмотр комнат

```bash
# Получить доступные комнаты для отеля
curl "http://localhost:8080/api/rooms/search?hotelId=1&startDate=2025-11-01&endDate=2025-11-05"
```

#### Бронирование комнаты

```bash
# Создать бронирование (требуется JWT токен)
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "roomId": 1,
    "startDate": "2025-11-10",
    "endDate": "2025-11-15"
  }'
```

## Настройка загрузки данных

### Отключение автоматической загрузки

Если вы хотите отключить автоматическую загрузку тестовых данных, вы можете:

1. **Удалить классы DataLoader:**
   - `hotel-service/src/main/java/com/booking/hotel/config/DataLoader.java`
   - `booking-service/src/main/java/com/booking/service/config/DataLoader.java`

2. **Или добавить условие в application.yml:**

```yaml
# В application.yml
test-data:
  enabled: false
```

И обновить DataLoader:

```java
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "test-data.enabled", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {
    // ...
}
```

### Изменение тестовых данных

Вы можете изменить тестовые данные, отредактировав соответствующие классы DataLoader. После изменения просто перезапустите сервисы.

## Важные замечания

1. **In-Memory Database:** Данные хранятся в памяти и будут потеряны при перезапуске сервисов.

2. **ddl-auto: create-drop:** Схема базы данных пересоздается при каждом запуске, что означает, что все данные будут заново загружены.

3. **Пароли:** Все пароли хешируются с помощью BCrypt перед сохранением в базу данных.

4. **UUID Request IDs:** Каждое бронирование получает уникальный requestId для обеспечения идемпотентности.

5. **Временные метки:** Поля createdAt устанавливаются автоматически с помощью @PrePersist.
