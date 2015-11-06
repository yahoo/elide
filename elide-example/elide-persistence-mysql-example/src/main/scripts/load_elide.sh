#!/bin/bash

# Loads the locally running Elide instance with some books and authors

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "-",
    "type": "author",
    "attributes": {
      "name": "Ernest Hemingway"
    }
  }
}' -X POST http://localhost:4080/author

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "type": "book",
    "attributes": {
      "title": "The Old Man and the Sea",
      "genre": "Literary Fiction",
      "language": "English"
    }
  }
}' -X POST http://localhost:4080/book

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "1",
    "type": "author"
  }
}' -X PATCH http://localhost:4080/book/1/relationships/authors

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "type": "book",
    "attributes": {
      "title": "The Old Man and the Sea",
      "genre": "Literary Fiction",
      "language": "English"
    },
    "relationships": {
      "authors": {
        "data": [
          {
            "type": "author",
            "id": "1"
          }
        ]
      }
    }
  }
}' -X POST http://localhost:4080/book

# Works
curl -H'Content-Type: application/vnd.api+json; ext=jsonpatch' \
     -H'Accept: application/vnd.api+json; ext=jsonpatch' --data '
[
  {
    "op": "add",
    "path": "/author",
    "value": {
      "id": "12345678-1234-1234-1234-1234567890ab",
      "type": "author",
      "attributes": {
        "name": "Ernest Hemingway"
      },
      "relationships": {
        "books": {
          "data": [
            {
              "type": "book",
              "id": "12345678-1234-1234-1234-1234567890ac"
            }
          ]
        }
      }
    }
  },
  {
    "op": "add",
    "path": "/book",
    "value": {
      "type": "book",
      "id": "12345678-1234-1234-1234-1234567890ac",
      "attributes": {
        "title": "The Old Man and the Sea",
        "genre": "Literary Fiction",
        "language": "English"
      }
    }
  }
]
' -X PATCH http://localhost:4080/

# Works
curl -H'Content-Type: application/vnd.api+json; ext=jsonpatch' \
     -H'Accept: application/vnd.api+json; ext=jsonpatch' --data '
[
  {
    "op": "add",
    "path": "/author",
    "value": {
      "id": "12345678-1234-1234-1234-1234567890ab",
      "type": "author",
      "attributes": {
        "name": "Ernest Hemingway"
      }
    }
  },
  {
    "op": "add",
    "path": "/book",
    "value": {
      "type": "book",
      "id": "12345678-1234-1234-1234-1234567890ac",
      "attributes": {
        "title": "The Old Man and the Sea",
        "genre": "Literary Fiction",
        "language": "English"
      },
      "relationships": {
        "authors": {
          "data": [
            {
              "type": "author",
              "id": "12345678-1234-1234-1234-1234567890ab"
            }
          ]
        }
      }
    }
  }
]
' -X PATCH http://localhost:4080/

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "-",
    "type": "book",
    "attributes": {
      "title": "For Whom the Bell Tolls",
      "genre": "Literary Fiction",
      "language": "English"
    }
  }
}' -X POST http://localhost:4080/book

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "1",
    "type": "author"
  }
}' -X PATCH http://localhost:4080/book/2/relationships/authors

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "-",
    "type": "author",
    "attributes": {
      "name": "Orson Scott Card"
    }
  }
}' -X POST http://localhost:4080/author

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "-",
    "type": "book",
    "attributes": {
      "title": "Enders Game",
      "genre": "Science Fiction",
      "language": "English"
    }
  }
}' -X POST http://localhost:4080/book

curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
  "data": {
    "id": "2",
    "type": "author"
  }
}' -X PATCH http://localhost:4080/book/3/relationships/authors
