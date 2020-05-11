Client.kt -- Клиент
Server.kt -- Сервер

Сервер протестирован с помощью утилиты tftp. Клиент с помощью сервера.

Запуск сервера ```./gradlew -PrunWorkingDir='server_data_path' '--console=plain run --args='port_number'```

К примеру ```./gradlew -PrunWorkingDir='server' --console=plain run --args='6969'```

Запуск клиента: ```./gradlew -Pclient -PrunWorkingDir='client_data_path' --console=plain run --args='host port mode'```

К примеру ```./gradlew -Pclient -PrunWorkingDir='client' --console=plain run --args='localhost 6969 netascii'```

Также во время работы клиент принимает команды put или get, после которых запрашивает имя файла.

Клиент выходить умеет только по сигналам

![1](/1.png)
![2](/2.png)
![3](/3.png)
