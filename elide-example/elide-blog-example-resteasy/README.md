# Elide Example

## Usage

1. Install and start a MySQL server

2. Create ```elide``` database

        mysql> create database elide;

3. Create ```elide``` user with password ```elide123```

        mysql> grant all on elide.* to 'elide'@'localhost' identified by 'elide123';

4. Launch the example webservice

        ~/elide $ mvn install
        ~/elide $ cd elide-example/elide-blog-example
        ~/elide/elide-example/elide-blog-example $ mvn exec:java -Dexec.mainClass="com.yahoo.elide.example.Main"

5. Create an admin user

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
          ' -X POST http://localhost:4080/user

6. Create a registered user

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
          ' -X POST http://localhost:4080/user

7. Create an unregistered user

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
          ' -X POST http://localhost:4080/user

8. Create a post as an admin:

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "post",
              "attributes": {
                "content": "The greatest thing ever by Michael",
                "me": "1"
              },
              "relationships": {
                "author": {
                  "data": {
                    "type": "user",
                    "id": "1"
                  }
                }
              }
            }
          }
          ' -X POST http://localhost:4080/post

9. Create a post as a registered user:

        $ curl -H'Content-Type: application/vnd.api+json' \
               -H'Accept: application/vnd.api+json' --data '
          {
            "data": {
              "type": "post",
              "attributes": {
                "content": "The 2nd best thing ever by Ken",
                "me": "2"
              },
              "relationships": {
                "author": {
                  "data": {
                    "type": "user",
                    "id": "2"
                  }
                }
              }
            }
          }
          ' -X POST http://localhost:4080/post

You can also load some data using `load_blog.sh` in `src/main/scripts/`
