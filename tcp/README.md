### Протокол

Бинарный протокол. Целые числа записываются в порядке little-endian.

Сообщение:

- 8 bytes - int64 time (unix timestamp)
- 8 bytes - uint64 name_length
- name_length bytes - name
- 8 bytes - uint64 text_length
- text_length bytes - text

### Аргументы

Сервер: port

Клиент: username hostname port

### Завершение

Для завершения нужно ввести exit (и для сервера, и для клиента).