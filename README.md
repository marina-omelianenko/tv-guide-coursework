# Телепрограма

Курсова робота, тема 23.

Вебзастосунок для перегляду телепрограми. Користувач може переглядати передачі, фільтрувати їх за каналом і сортувати за часом або каналом. Адміністратор може додавати, редагувати та видаляти канали і передачі.

## Запуск в IntelliJ IDEA

1. Відкрити папку проєкту в IntelliJ IDEA.
2. Запустити клас:

```text
src/main/java/ua/course/tvguide/TvGuideApplication.java
```

3. Після запуску відкрити в браузері:

```text
http://localhost:8080
```

## Адміністратор

```text
Логін: admin
Пароль: admin123
```

## Запуск через термінал

```bash
sh scripts/run.sh
```

## Відкриття з телефона

Телефон і комп'ютер мають бути в одній Wi-Fi мережі.

На телефоні потрібно відкривати не `localhost`, а IP-адресу комп'ютера, наприклад:

```text
http://192.168.1.10:8080
```

IP-адресу Mac можна подивитися в налаштуваннях Wi-Fi.

## Основні файли

```text
src/main/java/ua/course/tvguide/TvGuideApplication.java
src/main/java/ua/course/tvguide/model/
src/main/java/ua/course/tvguide/repository/
src/main/java/ua/course/tvguide/web/
src/main/resources/public/
data/
```
