# Elide Example for Redis

## Usage

1. Install and start a Redis server

2. Disable protected mode in the Redis server

        CONFIG SET protected-mode no
        
3. Launch the example webservice

        ~/elide $ mvn install
        ~/elide $ cd elide-example/elide-hibernate5-redis-example
        ~/elide/elide-example/elide-hibernate5-redis-example $ mvn exec:java -Dexec.mainClass="com.yahoo.elide.example.persistence.Main"

4. Create an author

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
          "data": {
            "id": "-",
            "type": "author",
            "attributes": {
              "name": "Ernest Hemingway"
            }
          }
        }' -X POST http://localhost:4080/author

5. Create a book

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
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

6. Associate the author and book

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
          "data": {
            "id": "1",
            "type": "author"
          }
        }' -X PATCH http://localhost:4080/book/1/relationships/authors

7. Get books

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' http://localhost:4080/book/

You can also load some pre-configured authors and books using `load_elide.sh` in `src/main/scripts/`
