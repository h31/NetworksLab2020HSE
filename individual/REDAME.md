# Multi-client chat using TSP 

## Protocol description

|  | body length  | UNIX-time (ms)  | username length | username  | message length | message |
| :---         |     :---:      |  :---:  |  :---:  |  :---:  |  :---:  |  :---:  |
| length (byte)   | 4 | 4 | 4  | `username length`  | 4 | `message length`|
| encoding  | BIG ENDIAN  | BIG ENDIAN | BIG ENDIAN | UTF-8 | BIG ENDIAN | UTF-8 | 