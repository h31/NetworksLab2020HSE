
# Message Format

|              | header | time | name | text  |
|--------------|--------|------|------|-------|
| size (bytes) | 4      | 8    | 32   | <=512 |


## Notes
* text and name should be ASCII and cant include "new line" character
* text max size is 511
* name max size is 31
