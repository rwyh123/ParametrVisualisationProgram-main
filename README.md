# ParametrVisualisationProgram

Система, которая собирает, обрабатывает и визуализирует телеметрические данные со станков с ЧПУ  в реальном времени. Предназначена для глубокого мониторинга состояния производственного оборудования и построения интерактивных дашбордов.

### Справка:
MT Connect, открытый и бесплатный протокол, увеличивает совместимость между оборудованием с программными приложениями разных производителей. MTConnect является протоколом только для чтения, то есть он только считывает данные с контролируемых устройств, что делает его подходящим для автоматического сбора данных с парка оборудования.

К ключевым элементам, из которых состоит приложение MTConnect, относятся адаптер MTConnect, агент MTConnect и клиент MTConnect. Наше приложение выступает в роли **клиента MTConnect**. Оно непрерывно запрашивает данные с агента по протоколу HTTP (формат XML), парсит их, сохраняет в реляционную базу данных и транслирует в веб-интерфейс для оперативного контроля.

## Основные возможности:

- Непрерывный опрос MTConnect агента с частотой 50 мс для обеспечения максимальной плавности потока данных.

- Извлечение данных  с помощью XPath и сохранение истории показателей в базу данных PostgreSQL.

- REST-API и Server-Sent Events: трансляция телеметрии `/stream` на клиент без перезагрузки страниц.

- Динамические графики с настраиваемыми пороговыми значениями  для отслеживания перегрева или превышения оборотов.

- Индустриальный веб-интерфейс.

## Стек технологий
- Язык: Java 17
- Фреймворк: Spring Boot Core, Web, Data JPA, Thymeleaf
- Сборка: Gradle
- Работа с данными: 
  - PostgreSQL + Hibernate — долговременное хранение метрик и структуры станка.
  - DOM/XPath — парсинг XML-потока MTConnect.
- Фронтенд: HTML5 Canvas, C3.js / D3.js, Vanilla JS, кастомные CSS-компоненты.

## Требования:

JDK: 17

Gradle Wrapper: ./gradlew

База данных: PostgreSQL 18 (БД: `MTConnectData`, пользователь: `appuser`, пароль: `rwyh123`)

OS: Windows / macOS / Linux

## Структура проекта в общих чертах:

    ParametrVisualisationProgram/
    ├─ src/
    │  ├─ main/java/org/example/
    │  │  ├─ Main.java                     # Точка входа
    │  │  ├─ DB/                           # Сервисы записи/чтения PostgreSQL и очистки БД
    │  │  ├─ ETL/                          # IngestService, ReaderService и маппинг единиц измерения
    │  │  ├─ Events/                       # Spring ApplicationEvents (передача ID новых записей)
    │  │  └─ Server/                       # SseController (/stream) и WebController (/cnc)
    │  └─ main/resources/
    │     ├─ application.yml               # Настройки БД, поллинга MTConnect и сервера
    │     ├─ thresholds.yml                # Настройки критических зон для графиков
    │     ├─ static/                       # CSS стили компонентов (кнопки, экраны) и JS логика (cnc.js)
    │     └─ templates/                    # Thymeleaf шаблоны (cnc.html)
    ├─ build.gradle
    ├─ settings.gradle
    └─ README.md

## **Быстрый старт локально**

### Подготовка базы данных:
Убедитесь, что у вас запущен PostgreSQL и создана база данных `MTConnectData`.

### Клонируем и собираем в терминал:

git clone https://github.com/rwyh123/ParametrVisualisationProgram.git

cd ParametrVisualisationProgram

./gradlew clean build

### Запускаем:

**Вариант A: через BootRun**

./gradlew bootRun

**Вариант B: через fat-jar** java -jar build/libs/ParametrVisualisationProgram-1.0-SNAPSHOT.jar

### Проверяем эндпоинт:

Веб-интерфейс: http://localhost:8080/cnc

SSE поток: http://localhost:8080/stream

## **Конфигурация:**

Все настройки в программе идут через YAML конфигурации.

### **src/main/resources/application.yml:**

    server:
      port: 8080

    spring:
      datasource:
        driver-class-name: org.postgresql.Driver
        url: jdbc:postgresql://localhost:5432/MTConnectData
        username: appuser
        password: rwyh123
      jpa:
        hibernate:
          ddl-auto: update
        show-sql: false
      thymeleaf:
        cache: false

    mtconnect:
      url: http://localhost:5050/mtconnect/current
      poll-ms: 50
      ingest-ms: 50
      http:
        connect-timeout-ms: 2000
        read-timeout-ms: 4000

    app:
      clear-db-on-start: true

### Настройка порогов (красных зон) для графиков
### **src/main/resources/thresholds.yml** пример:
    cnc:
      thresholds:
        z_temp: 42.5
        y_temp: 40.5
        x_temp: 40.5
        spindle_temp: 56.0
        spindle_smoothed_speed_rpm: 3020.0
        spindle_speed_rpm: 3030.0
        ElectricCurrent_1: 12.5
        Voltage_1: 226.0
        Power_1: 6.2
        Torque_1: 25.5

## Контакты

https://t.me/Yakov_Legioncommander
