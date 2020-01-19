# MEMEGregator

Это репо содержит проект в рамках
https://funcodechallenge.com/task


В основе лежат два сервиса :
1. Meme Collector Server - собирает мемы, записывает пишет видео и пикчи в s3, после чего сохраняет
метаданные в mongo.
2. Meme Query Server - реализует ендпоинты для получения метаданных, а так же видео и пикч.

# Общая информация

1. Поддерживаются видео и пикчи
1. Весь сервис полностью неблокирующий и основывается на WebFlux.
Используя всего 1 ядро + 250 мб хипа ( ещё ~50мб оффхипа, т.к. нетти создаёт буферы в оффхипе и тд)
полностью утилизируется сеть, высасывая мемы со скоростью 368 мбит/с (https://ibb.co/wrXYK6B)
1. Минимальная буферизация в памяти, обеспечивая максимально потоковую обработку поверх http.
Для этого обеспечивается асинхронный клиент S3 с поддержкой backpressure ( см. S3ContentStorage)
1. Backpressure лежит в основе приложения. Каждый элемент выдаёт ровно столько, сколько у него запросили.
 Ситуация что один элемент задудосил всё приложение ( например, начали выкачивать  контент быстрее , чем может грузить в s3) - отсутствует


## MemeCollector
Реализован из 4х основных компонентов :
1. Scrapper - собирает мемы с внешний сайтов и возвращает Flux с ExternalMemeContent. Изначально я выбрал не самый удачный источник мемасов,
в частности debeste.de. Сейчас я бы взял апи редита, т.к. там можно было бы очень гибко настроить какие треды собирать, какие языки и тд.
Так или иначе Scrapper устроен следующим образом : В mongo хранится таблица с оффсетами на которых остановился скраппер.
Там хранится следующее :
    1. Оффсет, на котором была законченая сборка ( закомиченный оффсет)
    1. Чекпоинт - страница и оффсет в рамках которых идёт сборка на текущий момент.
    1. флаг, показывающий был ли оффсет закоммичен.
    Общий алгоритм работы такой :
    1. При инициализации берём оффсеты из бд.
    1. Если их нет - значит сбора небыло и будем собирать до 0го id либо редиректа
    ( на debeste порядка 7к страниц, если запрашиваем больше чем есть - получаем 302, значит - всё что есть собрали)
    1. Собираем страницу. Записываем в чекпоинт что закончили на такой-то странице, ставим флаг что оффсеты на закоммичены.
    Если приложения будет остановлено и запущено заного без коммита оффсета - сбор будет возобновлён
    с чекпоинта а не с закомиченный оффсетов.
    1. Если нашли конечный индекс коммитим оффсет.
    При запуске с закомиченным оффсетом приложения будет собирать мемы до первого индекса, который будет соотвествовать тому, который в оффсете.

Сам скраппер так же обеспечивает backpressure. Внутри используется последовательная очередь событий, которые нужно обработать.
Минимальное количество мемов которое может быть собрано - одна страница ( 10 мемов).
На уровне reactivestreams есть контракт что нужно выдать ровно столько элементов, сколько запрошено реквестом.
Как следствие лишние мемы помещаются в очередь до момента пока они потребуется следующим реквестом.

2. Collector - выкачивает пикчи и внешние видео в S3. Получает Flux из ExternalMemeContent и преобразует его в InternalMemeContent.
Тут в основном используются два элемента HttpPuller + ContentStorage. Отправка в S3 так же полностью асинхронная и поддерживает backpressure.
Это обеспечивается за счёт Spring WebClient( который основан на netty) и в ручную реализованного собскрайбера внутри S3ContentStorage,
 который запрашивает данные только когда они нужны. Это защищает от ситуации когда выкачивание большого количества контента взорвёт наше приложение,
 если контент не будет успевать записывать в s3.
 Тут же считается хэш контента ( это делается так же потоково)

3. Publisher - Записывает мета информацию в mongodb. В случае если запись провалилась ( например запись с таким хэшем уже есть)
данные удаляются из s3. Именно этот элемент запрашивает мемесы от предыдущих элементов и обеспечивает backpressure в основной цепочке.

## Meme Query Server
Подключается к таблице с мета информацией и выдаёт как ленту, так и контент.
Основные engpointы -
  /feed, имеет параметры startId - начальный id, после которого нужно найти мемы,
  endId - конечный id, до которого нужно найти мемы
  limit - количество мемов которое вернуть
  /image/{id} - возвращает пикчу с нужным id
  /video/{id} - возвращает видео с нужным id

  Оба ендпоинта /image и /video поддерживают Range запросы. В целом работает следующим образом :
  * Получаем запрос на видео.
  * Идём в s3, передаём range который нам нужен.
  * Получаем хедеры, выдаём content-length клиенту и потоково выдаём тело
  ( буферизации в памяти по минимуму + backpressure)

Весь функционал на просмотр видео работает как минимум в хроме( используя html тег video), поддерживая перемотку и тд.

## Метрики
Метрики приложения на текущий момент выдаются для jmx и для prometheus
В случае прометеуса они доступны по пути /actuator/prometheus
К сожелению очень мало времени и выводил туда в основном когда искал где что задыхается

# Хелсчеки
Доступны по пути /actuator/health

## Чего не хватает
Чего тут нет, но то как это должно быть :
* OpenApi. Тут пишем json и выдаём его на одном из каких-нибудь endpoint . Для spring mvc ещё есть кашерный springfox, но для webflux он пока что не поддердживается.
* Тестов. Тут всё просто, что-то мокаем и тестируем большинство классов юнит тестами. Оба сервиса так же легко тестируются интеграционно,
для meme query в спринге ещё максимально просто запустить полноценный сервис внутри тестов

Так же из-за нехватки времени в приложении остаётся много захардкоженных штук, практически нет комментариев :C


## Запуск из IDE
При клонировании репы и открытии в IDEA сразу будут доступны две зашаренные конфигурации запуска.
Так же есть 2 конфигурации для сборки и запуска непосредственно контейнеров
Нужно только заполнить параметры в /configuration/collector.properties и /configuration/query.properties
В случае настройки конфигураций для докера, там нужно вписать тоже самое в env переменных.
В таком случае имя параметра пишется капсом, точки заменяются на подчёркивание
aws.secretKey => AWS_SECRETKEY="hohohaha"

## Параметры для запуска
Параметров не так много :
```
# keys for aws
aws.accessKey=HOHOHAHA
aws.secretKey=HEHEHEHEHEHEHE
# s3 bucket which is used to store all the memes
s3.bucketName=memegregator-indexing
# mongo connection string
mongo.connectionString=mongodb://localhost:27017
# mongo database which will hold require collections
mongo.databaseName=memegregator
# collections with offsets/checkpoints
mongo.offsetCollection=offsets
# collection with metadata
mongo.memeCollection=memes


#Metrics related configurations
management.endpoint.metrics.enabled=true
management.endpoints.web.exposure.include=*
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
```

## Индексы
В монго для таблицы с мета информацией мемасов(mongo.memeCollection) должен быть уникальный индекс на content.hash. Его нужно создать в ручную

## Запуск вне IDE
1) Заполняем конфиги
2) Собираем проект , на выходе в target директории получаем 2 jarника
3) Запускаем каждый из них. Тут в целом всё дефолтно

Collector Server
```
java -jar meme-collector-server-0.0.1.jar -Xmx250m -Xms250m -XX:+UseG1GC --spring.config.location=../../configuration/collector.properties --logging.config=../../configuration/logback-spring.xml --server.port=8081
```

Query Server
```
java -jar meme-query-server-0.0.1.jar -Xmx250m -Xms250m -XX:+UseG1GC --spring.config.location=../../configuration/query.properties --logging.config=../../configuration/logback-spring.xml --server.port=8080
```

Всё основывается на конфигурациях спринга и все параметры можно передать так же напрямую, либо через environment переменные


## Запуск в докерах
Все параметры передаются через environment переменные, например :

В папках meme-collector-server и meme-query-server находятся два соотвествующих файла.
Предполагается что сборка запускается из этих папок
Так же нужно проставить переносы строк в конце в зависимости от вашей ОС =)
### Query Server
```
cd meme-query-server

docker build -t memegregator/query-server:latest .
&& docker run
-p 8080:8080
--env MONGO_CONNECTIONSTRING=mongodb://host.docker.internal:27017
--env MONGO_DATABASENAME=memegregator
--env AWS_SECRETKEY=YOUSUPERSECRETKEY
--env AWS_ACCESSKEY=YOURSUPERACCESSKEY
--env S3_BUCKETNAME=memegregator-indexing
--env MANAGEMENT_ENDPOINT_METRICS_ENABLED=true
--env MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
--env MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED=true
--env MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
--name memegregator-query-server
memegregator/query-server:latest
```


### Collector Server
```
cd meme-collector-server

docker build -t memegregator/collector-server:latest .
&& docker run
-p 8081:8081
--env MONGO_CONNECTIONSTRING=mongodb://host.docker.internal:27017
--env MONGO_DATABASENAME=memegregator
--env AWS_SECRETKEY=YOURSUPERSECRETKEY
--env AWS_ACCESSKEY=YOURSUPERACCESSKEY
--env S3_BUCKETNAME=memegregator-indexing
--env MANAGEMENT_ENDPOINT_METRICS_ENABLED=true
--env MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
--env MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED=true
--env MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
--name memegregator-collector-server
memegregator/collector-server:latest
```