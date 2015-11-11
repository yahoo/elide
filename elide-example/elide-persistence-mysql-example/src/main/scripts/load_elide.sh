#!/bin/bash

# Loads the locally running Elide instance with some books and authors

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
            },
            {
              "type": "book",
              "id": "12345678-1234-1234-1234-1234567890ad"
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
  },
  {
    "op": "add",
    "path": "/book",
    "value": {
      "type": "book",
      "id": "12345678-1234-1234-1234-1234567890ad",
      "attributes": {
        "title": "For Whom the Bell Tolls",
        "genre": "Literary Fiction",
        "language": "English"
      }
    }
  }
]
' -X PATCH http://localhost:4080/

curl -H'Content-Type: application/vnd.api+json; ext=jsonpatch' \
     -H'Accept: application/vnd.api+json; ext=jsonpatch' --data '
[
  {
    "op": "add",
    "path": "/author",
    "value": {
      "id": "12345679-1234-1234-1234-1234567890ab",
      "type": "author",
      "attributes": {
        "name": "Orson Scott Card"
      },
      "relationships": {
        "books": {
          "data": [
            {
              "type": "book",
              "id": "12345679-1234-1234-1234-1234567890ac"
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
      "id": "12345679-1234-1234-1234-1234567890ac",
      "attributes": {
      "title": "Enders Game",
      "genre": "Science Fiction",
      "language": "English"
      }
    }
  }
]
' -X PATCH http://localhost:4080/

curl -H'Content-Type: application/vnd.api+json; ext=jsonpatch' \
     -H'Accept: application/vnd.api+json; ext=jsonpatch' --data '
[
  {
    "op": "add",
    "path": "/author",
    "value": {
      "id": "12345680-1234-1234-1234-1234567890ab",
      "type": "author",
      "attributes": {
        "name": "Isaac Asimov"
      },
      "relationships": {
        "books": {
          "data": [
            {
              "type": "book",
              "id": "12345680-1234-1234-1234-1234567890ac"
            },
            {
              "type": "book",
              "id": "12345680-1234-1234-1234-1234567890ad"
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
      "id": "12345680-1234-1234-1234-1234567890ac",
      "attributes": {
      "title": "Foundation",
      "genre": "Science Fiction",
      "language": "English"
      }
    }
  },
  {
    "op": "add",
    "path": "/book",
    "value": {
      "type": "book",
      "id": "12345680-1234-1234-1234-1234567890ad",
      "attributes": {
      "title": "The Roman Republic",
      "genre": "History",
      "language": "English"
      }
    }
  }
]
' -X PATCH http://localhost:4080/
