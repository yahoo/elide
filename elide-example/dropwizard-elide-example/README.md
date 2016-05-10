# Dropwizard Elide Example
This example demonstrates how to use the dropwizard-elide bundle.

## Usage

1. Install and start a MySQL server

2. Create ```elide``` database

        mysql> create database elide;

3. Create ```elide``` user with password ```elide123```

        mysql> grant all on elide.* to 'elide'@'localhost' identified by 'elide123';

4. Launch the example webservice

        ~/elide $ mvn install
        ~/elide $ cd elide-example/dropwizard-elide-example
        ~/elide/elide-example/dropwizard-elide-example $ java -jar target/dropwizard-elide-example-{version}-SNAPSHOT.jar server example.yml

   Replace {version} with appropriate value, which can be found under ./target

   The output of the last command will have a line at the very bottom like this

        INFO  [2016-05-07 21:25:09,453] org.eclipse.jetty.server.ServerConnector: Started application@***{HTTP/1.1}{0.0.0.0:{port}}

   The application starts listening at port {port}. This port number is specified in [configuration file](https://github.com/yahoo/elide/blob/master/elide-example/dropwizard-elide-example/example.yml). Let's take {port}=4080 for an example.

5. Create an author

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
          "data": {
            "id": "-",
            "type": "author",
            "attributes": {
              "name": "Ernest Hemingway"
            }
          }
        }' -X POST http://localhost:4080/author

6. Create a book

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

7. Associate the author and book

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' --data '{
          "data": {
            "id": "1",
            "type": "author"
          }
        }' -X PATCH http://localhost:4080/book/1/relationships/authors

8. Get books

        $ curl -H'Content-Type: application/vnd.api+json' -H'Accept: application/vnd.api+json' http://localhost:4080/book/

You can also load some pre-configured authors and books using `load_elide.sh` in `/scripts`
