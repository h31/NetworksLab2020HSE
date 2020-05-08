Запуск сервера: `./gradlew run --args='--server'`

Запуск клиента: `./gradlew run --args='--client'`

Формат запросов в клиенте: `<read|write> <local_file> tftp://remote_host/remote_file`

Пример: `read test.txt tftp://localhost/build.gradle`