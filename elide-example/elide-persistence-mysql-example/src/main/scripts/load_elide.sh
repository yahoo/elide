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
    "id": "-",
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
