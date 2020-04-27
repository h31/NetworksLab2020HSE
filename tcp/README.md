## Протокол взаимодействия

### Формат сообщений, отправляемых клиентом серверу
| message_length| username_length | message              |   username             |
| ------------- | ----------------|----------------------|------------------------|
| uint32_t      | uint32_t        | $message_length bytes| $username_length bytes |

### Формат сообщений, отправляемых сервером клиенту

|time_stamp| message_length| username_length | message              |   username             |
|----------| ------------- | ----------------|----------------------|------------------------|
| time_t   | uint32_t      | uint32_t        | $message_length bytes| $username_length bytes |
