Реализация [RFC-1350](https://tools.ietf.org/html/rfc1350)

Для компиляции и запуска нужна JDK 11.

Запуск сервера:

`./gradlew runServer [-Pport=PORT_NUM]`

Прочитать файл с сервера:

`./gradlew clientGet -Pfile=FILE_NAME -Pip=SERVER_IP [-Pmode=(OCTET/NETASCII)] [-Pport=SERVER_PORT]`

Загрузить файл на сервер:

`./gradlew clientPut -Pfile=FILE_NAME -Pip=SERVER_IP [-Pmode=(OCTET/NETASCII)] [-Pport=SERVER_PORT]`

Работоспособность сервера протестирована на OS Linux с помощью консольной утилиты `tftp` из пакета Linux NetKit 0.17 
(есть в стандартных репозиториях Ubuntu)

Работоспособность клиента протестирована на OS Linux с помощью сервера `tftpd-hpa` версии 5.2 
(есть в стандартных репозиториях Ubuntu)

