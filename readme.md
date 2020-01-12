# MEMEGregator

Это репо содержит проект в рамках
https://funcodechallenge.com/task


В основе лежат два сервиса :
1. Meme Collector Server - собирает мемы, записывает пишет видео и пикчи в s3, после чего сохраняет
метаданные в mongo.
2. Meme Query Server - реализует ендпоинты для получения метаданных, а так же видео и пикч.

# Общая информация

1.1. Поддерживаются видео и пикчи
1.2. Весь сервис полностью неблокирующий и основывается на WebFlux.
Используя всего 1 ядро + 250 мб хипа ( потенциально ещё ~50мб оффхипа, т.к. нетти создаёт буферы в оффхипе)
полностью утилизируется сеть, высасывая мемы со скоростью 368 мбит/с (https://ibb.co/wrXYK6B)
1.3. Минимальная буферизация в памяти, обеспечивая максимально потоковую обработку поверх http.
Для этого обеспечивается асинхронный клиент S3 с поддержкой backpressure ( см. S3ContentStorage)
1.4 Backpressure лежит в основе приложения. Каждый элемент выдаёт ровно столько, сколько у него запросили.
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



## О том насколько всё завершено
В целом всё работает, но до продакшен реди приложения тут ещё очень далеко.
На создание полностью готового приложения со всеми метриками/логами/трейсингом/упаковкой,
 полностью покрытым различными тестами и описанием сваггер ендпоинтов нужно много времени и совмещать
 с fulltime работой крайне сложно. Но так или иначе я всё равно решил это отправить ( как никак все выхи просидел разбираясь со стеком :D )
Чего тут нет, но то как это должно быть :
* Метрики. Они на самом деле в некоторой степени есть, в основном те, которые я использовал для определения
  где что тормозит/забивает хип. Используется micrometer, выводится в jmx.
  Чтобы вывести для прометеуса нужно просто добавить регистр prometheus в мавен зависимость.
* OpenApi. Тут пишем json и выдаём его на одном из каких-нибудь endpoint . Для spring mvc ещё есть кашерный springfox, но для webflux он пока что не поддердживается.
* Docker образа
* Тестов. Тут всё просто, что-то мокаем и тестируем большинство классов юнит тестами. Оба сервиса так же легко тестируются интеграционно,
для meme query в спринге ещё максимально просто запустить полноценный сервис внутри тестов


## Запуск из IDE
При клонировании репы и открытии в IDEA сразу будут доступны две зашаренные конфигурации запуска.
Нужно только заполнить параметры в /configuration/collector.properties и /configuration/query.properties

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
```

## Запуск вне IDE
Т.к. времени упаковать в докер не осталось, запустить можно используя только традиционные способы
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