# Телепрограма

Курсова робота з програмування, тема 23: вебзастосунок для перегляду телепрограми та керування каналами і передачами.

## Функції

- Користувач переглядає список передач.
- Користувач фільтрує розклад за каналом.
- Користувач сортує передачі за часом або за каналами.
- Адміністратор додає, редагує та видаляє канали.
- Адміністратор додає, редагує та видаляє передачі.
- Дані зберігаються у файлах `data/channels.tsv` і `data/programs.tsv`.

## Технології

- Java 21.
- Вбудований HTTP-сервер `com.sun.net.httpserver.HttpServer`.
- HTML, CSS, JavaScript.
- Файлове сховище TSV без зовнішніх бібліотек.

## Запуск

### В IntelliJ IDEA

1. Відкрий IntelliJ IDEA.
2. Обери `File` -> `Open`.
3. Вибери папку проєкту:

```text
/Users/mirinam./Documents/курсовая
```

4. Якщо IntelliJ запитає, як відкрити проєкт, обери `Open as Project`.
5. Дочекайся індексації Maven-проєкту.
6. У верхній панелі обери конфігурацію `TvGuideApplication`.
7. Натисни зелену кнопку запуску.
8. Відкрий у браузері:

```text
http://localhost:8080
```

Альтернативно можна відкрити файл `src/main/java/ua/course/tvguide/TvGuideApplication.java` і натиснути зелений трикутник біля методу `main`.

### Через термінал

```bash
sh scripts/run.sh
```

Після запуску відкрий:

```text
http://localhost:8080
```

Дані адміністратора:

```text
Логін: admin
Пароль: admin123
```

Можна передати інший порт:

```bash
sh scripts/run.sh 9090
```

## Структура

```text
src/main/java/ua/course/tvguide/
  TvGuideApplication.java          головний клас запуску
  model/Channel.java               модель телеканалу
  model/Program.java               модель телепередачі
  repository/TvGuideRepository.java робота з даними
  web/ApiHandler.java              REST API
  web/JsonUtil.java                простий JSON-помічник
  web/StaticFileHandler.java       видача HTML/CSS/JS

src/main/resources/public/
  index.html                       інтерфейс застосунку
  styles.css                       оформлення
  app.js                           логіка сторінки

data/
  channels.tsv                     канали
  programs.tsv                     передачі
```

## API

```text
GET    /api/channels
POST   /api/channels
PUT    /api/channels/{id}
DELETE /api/channels/{id}

GET    /api/programs?channelId=1&sort=time
POST   /api/programs
PUT    /api/programs/{id}
DELETE /api/programs/{id}

POST   /api/login
GET    /api/health
```

Запити на створення, редагування та видалення потребують заголовок `X-Admin-Token`, який сторінка отримує після входу адміністратора.
