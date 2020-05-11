Порт по умолчанию: 69  
Все данные хранятся в ./SERVER_STORAGE  
  
Для запуска сервера:  
  
$ ./gradlew run --console=plain --args='server [port]'  
Например  
$ ./gradlew run --console=plain --args='server'  
$ ./gradlew run --console=plain --args='server 1234'  
  
Для запуска клиента:  
$ ./gradlew run --console=plain --args='client <read|write> <filename> <server ip> [server port]'  
  
Например:  
$ ./gradlew run --console=plain --args='client read 1.txt 127.0.0.1'  
$ ./gradlew run --console=plain --args='client write 1.txt 127.0.0.1 1234'  
