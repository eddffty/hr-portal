# REST API HR-портала

Базовый URL запущенного приложения: `http://localhost:8080/api` (в текущем демонстрационном запуске порт 8081). Все тела запросов и ответы — JSON. В Postman можно использовать эти же маршруты.

## Авторизация

`POST /login` с телом:

```json
{"login":"glukhov","password":"Pavel2026!"}
```

Ответ — профиль без хеша пароля. Заголовок ответа `X-Portal-Token` содержит токен; передавайте его во всех последующих запросах:

```http
Authorization: Bearer <токен>
```

Токен действует два часа. `POST /logout` отзывает его. Сервер хранит SHA-256 токена в PostgreSQL; сам секрет в БД не записывается. Роль и владелец операции берутся из сессии, а не из переданного клиентом логина. Существующая cookie-сессия поддерживается для совместимости; для новых клиентов используйте Bearer.

Пример PowerShell:

```powershell
$loginResponse = Invoke-WebRequest http://localhost:8081/api/login -Method Post -ContentType application/json -Body '{"login":"glukhov","password":"Pavel2026!"}'
$portalHeaders = @{ Authorization = 'Bearer ' + $loginResponse.Headers['X-Portal-Token'][0] }
Invoke-RestMethod 'http://localhost:8081/api/shifts?building=1&mine=true' -Headers $portalHeaders
```

## Чтение ресурсов

| Метод и маршрут | Назначение |
| --- | --- |
| GET /profile | Текущий профиль |
| GET /rating | Демонстрационный рейтинг |
| GET /shifts?week=2026-11-23&building=1&mine=false | Смены недели, необязательные фильтры корпуса и собственных смен; по умолчанию текущая неделя |
| GET /shifts/{id} | Одна смена с владельцем, резервом и публикацией |
| GET /offers | Только доступные текущему пользователю предложения и его собственные публикации |
| GET /requests | Личные заявки; у начальника — очередь и история согласования |
| GET /notifications | Только личные уведомления |
| GET /time | Свои записи времени; начальник и ответственный видят все |
| GET /availability?user=glukhov | Занятость; сотрудник может читать только свою |
| GET /staff | Сотрудники, только начальник/ответственный |
| GET /planner/candidates/{shiftId} | Кандидаты с доступностью, только начальник/ответственный |
| GET /manager/requests | Очередь начальника |
| GET /state?week=2026-11-23 | Составной ответ для существующего интерфейса |

Поля смены: `id`, `date`, `start`, `end`, `slot` (0–7), `building` (1 или 4), `room`, `owner` (логин либо null), `ownerName`, `completed`, `locked`, `offerId`.

## Изменения

| Метод и маршрут | JSON-тело / действие |
| --- | --- |
| POST /offers/{shiftId} | Публикация своей смены, без тела |
| DELETE /offers/{offerId} | Снятие своей открытой публикации |
| POST /exchange/{offerId}/apply | Отклик, без тела; резерв до решения начальника |
| POST /exchange/{offerId}/decline | Персональный отказ, без тела |
| POST /proposals | `{"shiftIds":[123,124],"comment":"Готов дежурить"}` |
| POST /swaps | `{"ownShiftId":123,"targetShiftId":456,"comment":"Прошу обмен"}` |
| POST /requests/{id}/decision | `{"accept":true}` — решение получателя адресного обмена |
| POST /requests/{id}/cancel | Отмена инициатором, без тела |
| POST /manager/requests/{id} | `{"accept":false,"reason":"Причина отказа"}` — только начальник |
| POST /schedule/shifts | `{"date":"2026-12-07","slot":0,"building":1,"room":"1108","owner":null}` — ответственный |
| PUT /schedule/shifts/{id} | `{"owner":"glukhov"}` либо null для освобождения, ответственный |
| POST /schedule/publish | Уведомление о публикации, ответственный |
| POST /planner/save | `{"changes":[{"shiftId":123,"owner":"glukhov"}]}` — начальник/ответственный |
| PUT /availability | `{"parity":0,"cells":["1-0","1-1"],"description":"Пара"}` — сотрудник; parity 0 — чётная, 1 — нечётная, день 1–6 |
| DELETE /availability/{id} | Удалить свою занятость |
| POST /time/start/{shiftId} | Начать собственную сегодняшнюю смену |
| POST /time/stop/{entryId} | Завершить свою рабочую сессию |
| POST /notifications/read | Прочитать все личные уведомления |
| POST /staff | Создание сотрудника, только начальник/ответственный |
| PUT /staff | Редактирование сотрудника, только начальник/ответственный |

Точные поля `/staff`: см. `StaffService.Input`. Успешные команды возвращают HTTP 200 без тела; создание и редактирование не меняют контракт существующего интерфейса.

## Ошибки и транзакции

- 400 — некорректные параметры или JSON.
- 401 — требуется вход либо токен истёк.
- 403 — роль не разрешает действие.
- 404 — сущность не найдена.
- 409 — бизнес-конфликт: смена занята, пересечение времени, повторное решение, чужое предложение и т. п.

Каждая операция API выполняется в транзакции. Назначения, статус запроса, уведомления и аудит фиксируются вместе. При ошибке все изменения откатываются. PostgreSQL блокирует конкурирующие операции до завершения текущей транзакции; второй отклик проверяет уже обновлённое состояние.
