# Elide Example

An archetype Elide project for Heroku using Postgres DB.  

## Usage

1. Launch the example webservice

        ~/elide $ mvn install
        ~/elide $ cd elide-example/elide-blog-example
        ~/elide/elide-example/elide-blog-example $ mvn exec:java -Dexec.mainClass="com.yahoo.elide.example.Main"

2. Create an admin user

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "user",
              "attributes": {
                "name": "Michael Admin",
                "role": "Admin"
              }
            }
          }
          ' -X POST http://localhost:4080/api/v1/user

3. Create a registered user

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "user",
              "attributes": {
                "name": "Ken Registered",
                "role": "Registered"
              }
            }
          }
          ' -X POST http://localhost:4080/api/v1/user

4. Create an unregistered user

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "user",
              "attributes": {
                "name": "Shad Unreg",
                "role": "Unregistered"
              }
            }
          }
          ' -X POST http://localhost:4080/api/v1/user

5. Create a post as an admin:

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "post",
              "attributes": {
                "content": "The greatest thing ever by Michael",
                "me": "3"
              },
              "relationships": {
                "author": {
                  "data": {
                    "type": "user",
                    "id": "3"
                  }
                }
              }
            }
          }
          ' -X POST http://localhost:4080/api/v1/post

6. Create a post as a registered user:

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "post",
              "attributes": {
                "content": "The 2nd best thing ever by Ken",
                "me": "4"
              },
              "relationships": {
                "author": {
                  "data": {
                    "type": "user",
                    "id": "4"
                  }
                }
              }
            }
          }
          ' -X POST http://localhost:4080/api/v1/post
