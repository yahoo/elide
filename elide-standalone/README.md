Table of Contents
=================

  * [Overview](#overview)
  * [Who is this for?](#whofor)
  * [Getting Started](#gettingstarted)
  * [More Detailed Examples](#moredetail)

## <a name="overview"></a>Overview

The Elide standalone application is a configurable web server using Elide. While Elide is typically a pluggable **middleware** framework, we have constructed a flexible and complete service to allow you to get started quickly.

The Elide standalone application takes an opinionated stance on its technology stack (i.e. jersey/jetty), but provides many opportunities for users to configure the behavior of their application. To use the Elide standalone application, there are only a few steps:
  1. Configure ElideStandalone by implementing the ElideStandaloneSettings interface.
  1. Build an uber jar containing `elide-standalone`, your models, security checks, and additional application configuration.
  1. Start your web service:
     * `$ java -jar YOUR_APP.jar`

## <a name="whofor"></a>Who is this for?

The Elide standalone application is an alternative to Spring Boot for getting started quickly with Elide.  However, if you need more flexibility in your application than what is provided, then you should consider using the Elide __middleware__ directly.

## <a name="gettingstarted"></a>Getting Started

This tutorial will use elide-standalone, and all of the code is [available here](https://github.com/yahoo/elide-standalone-example).  You can deploy and play with this example on Heroku or locally.  The landing page will let you toggle between the [swagger UI](https://swagger.io/tools/swagger-ui) and [Graphiql](https://github.com/graphql/graphiql) for the example service.

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/yahoo/elide-standalone-example)

### Add Elide as a Dependency

To include `elide-standalone` into your project, add the single dependency:	
```xml	
<dependency>	
  <groupId>com.yahoo.elide</groupId>	
  <artifactId>elide-standalone</artifactId>	
  <version>LATEST</version>	
</dependency>	
```

### Create Models

Elide models are some of the most important code in any Elide project. Your models are the view of your data that you wish to expose. In this example we will be modeling a software artifact repository since most developers have a high-level familiarity with artifact repositories such as Maven, Artifactory, npm, and the like.
 
The first models we’ll need are `ArtifactGroup`, `ArtifactProduct`, and `ArtifactVersion`.  For brevity we will omit package names and import statements. 

#### ArtifactGroup.java

  ```java
  @Include(rootLevel = true, type = "group")
  @Entity
  public class ArtifactGroup {
      @Id
      public String name = "";

      public String commonName = "";

      public String description = "";

      @OneToMany(mappedBy = "group")
      public List<ArtifactProduct> products = new ArrayList<>();
  }
  ```

#### ArtifactProduct.java

  ```java
  @Include(type = "product")
  @Entity
  public class ArtifactProduct {
      @Id
      public String name = "";

      public String commonName = "";

      public String description = "";

      @ManyToOne
      public ArtifactGroup group = null;

      @OneToMany(mappedBy = "artifact")
      public List<ArtifactVersion> versions = new ArrayList<>();
  }
  ```

#### ArtifactVersion.java

  ```java
  @Include(type = "version")
  @Entity
  public class ArtifactVersion {
      @Id
      public String name = "";

      public Date createdAt = new Date();

      @ManyToOne
      public ArtifactProduct artifact;
  }
  ```

### Spin up the API

So now we have some models, but without an API it is not very useful. Before we add the API component, we need to create the schema in the database that our models will use.   Our example uses liquibase to manage the schema.  When Heroku releases the application, our example will execute the [database migrations](https://github.com/yahoo/elide-standalone-example/blob/master/src/main/resources/db/changelog/changelog.xml) to configure the database with some test data automatically.  This demo uses Postgres.  Feel free to modify the migration script if you are using a different database provider.

There may be more tables in your database than models in your project or vice versa.  Similarly, there may be more columns in a table than in a particular model or vice versa.  Not only will our models work just fine, but we expect that models will normally expose only a subset of the fields present in the database. Elide is an ideal tool for building micro-services - each service in your system can expose only the slice of the database that it requires.

### App & Settings

Bringing life to our API is trivially easy. We need two new classes: Main and Settings.

#### Main.java

  ```java
  public class Main {
      public static void main(String[] args) throws Exception {
          ElideStandalone app = new ElideStandalone(new Settings());
          app.start();
      }
  }
  ```

#### Settings.java

  ```java
  public class Settings implements ElideStandaloneSettings {
      /**
       * Tells elide where our models live.
       */
      @Override
      public String getModelPackageName() {
          return ArtifactGroup.class.getPackage().getName();
      }

      /**
       * Configuration properties for how to talk to the database.
       */
      @Override
      public Properties getDatabaseProperties() {
          Properties options = new Properties();
          
          //Here we use H2 in memory instead of Postgres 
          options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
          options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
          options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1");
          options.put("javax.persistence.jdbc.user", "sa");
          options.put("javax.persistence.jdbc.password", "");
             
          return options;
      }
  } 
  ```

### Supporting Files

Elide standalone uses a JPA data store (the thing that talks to the database) that is [configured programmatically](https://github.com/yahoo/elide-standalone-example/blob/master/src/main/java/example/Settings.java#L95-L111) (no persistence.xml required).

If you want to see the logs from your shiny new API, you will also want a [log4j config](https://github.com/yahoo/elide-standalone-example/blob/master/src/main/resources/log4j2.xml).
Your log4j config should go in `src/main/resources` so log4j can find it.

### Running

With these new classes, you have two options for running your project.  You can either run the `Main` class using your
favorite IDE, or you can run the service from the command line:

```mvn exec:java -Dexec.mainClass="example.Main"```

Our example requires the following environment variables to be set to work correctly with Heroku and Postgres.  

1. JDBC_DATABASE_URL
2. JDBC_DATABASE_USERNAME
3. JDBC_DATABASE_PASSWORD

If running inside a Heroku dyno, Heroku sets these variables for us.  If you don't set them, the example will use the H2 in memory database.

With the `Main` and `Settings` classes we can now run our API. 

You can now run the following curl commands to see some of the sample data that the liquibase migrations added for us:
Don't forget to replace localhost:8080 with your Heroku URL if running from Heroku!

#### JSON-API

  ```curl
  curl http://localhost:8080/api/v1/group
  ```

#### GraphQL
  ```curl
  curl -g -X POST -H"Content-Type: application/json" -H"Accept: application/json" \
      "http://localhost:8080/graphql/api/v1" \
      -d'{   
             "query" : "{ group { edges { node { name commonName description } } } }"
         }'
  ```

Here are the respective responses:

#### JSON-API
  ```json
    {
        "data": [
        {    
            "attributes": {
            "commonName": "Example Repository",
            "description": "The code for this project"
            },
            "id": "com.example.repository",
            "relationships": {
            "products": {
                "data": [
                {    
                    "id": "elide-demo",
                    "type": "product"
                }
                ]
            }
            },
            "type": "group"
        },
        {   
            "attributes": {
            "commonName": "Elide",
            "description": "The magical library powering this project"
            },
            "id": "com.yahoo.elide",
            "relationships": {
            "products": {
                "data": [
                {   
                    "id": "elide-core",
                    "type": "product"
                },
                {   
                    "id": "elide-standalone",
                    "type": "product"
                },
                {
                    "id": "elide-datastore-hibernate5",
                    "type": "product"
                }
                ]
            }
            },
            "type": "group"
        }
        ]
    }
  ```


#### GraphQL
  ```json
    {
        "data": {
            "group": {
                "edges": [
                {
                    "node": {
                    "commonName": "Example Repository",
                    "description": "The code for this project",
                    "name": "com.example.repository"
                    }
                },
                {
                    "node": {
                    "commonName": "Elide",
                    "description": "The magical library powering this project",
                    "name": "com.yahoo.elide"
                    }
                }
                ]
            }
        }
    }
  ```
### Looking at more data

You can navigate through the entity relationship graph defined in the models and explore relationships:

```
List groups:                 group/
Show a group:                group/<group id>
List a group's products:     group/<group id>/products/
Show a product:              group/<group id>/products/<product id>
List a product's versions:   group/<group id>/products/<product id>/versions/
Show a version:              group/<group id>/products/<product id>/versions/<version id>
```

### Writing Data

So far we have defined our views on the database and exposed those views over HTTP. This is great progress, but so far
we have only read data from the database.

#### Inserting Data

Fortunately for us adding data is just as easy as reading data. For now let’s use cURL to put data in the database.

#### JSON-API
  ```
  curl -X POST http://localhost:8080/api/v1/group/com.example.repository/products -H"Content-Type: application/vnd.api+json" -H"Accept: application/vnd.api+json" -d '{"data": {"type": "product", "id": "elide-demo"}}'
  ```

#### GraphQL
  ```
  curl -g -X POST -H"Content-Type: application/json" -H"Accept: application/json" "http://localhost:8080/graphql/api/v1" -d'{ "query" : "mutation { group(ids: [\"com.example.repository\"]) { edges { node { products(op: UPSERT, data: {name: \"elide-demo\"}) { edges { node { name } } } } } } }" }'
  ```

### Modifying Data

Notice that, when we created it, we did not set any of the attributes of our new product record.  Updating our
data to help our users is just as easy as it is to add new data. Let’s update our model with the following cURL call.

#### JSON-API
  ```curl
    curl -X PATCH http://localhost:8080/api/v1/group/com.example.repository/products/elide-demo \
      -H"Content-Type: application/vnd.api+json" -H"Accept: application/vnd.api+json" \
      -d '{
        "data": {
          "type": "product",
          "id": "elide-demo",
          "attributes": {
            "commonName": "demo application",
            "description": "An example implementation of an Elide web service that showcases many Elide features"
          }
        }
      }'
  ```

#### GraphQL
  ```curl
   curl -g -X POST -H"Content-Type: application/json" -H"Accept: application/json" \
      "http://localhost:8080/graphql/api/v1" \
      -d'{   
             "query" : "mutation { group(ids: [\"com.example.repository\"]) { edges { node { products(op: UPDATE, data: { name: \"elide-demo\", commonName: \"demo application\", description: \"An example implementation of an Elide web service that showcases many Elide features\" }) { edges { node { name } } } } } } }"
         }'
  ```

It’s just that easy to create and update data using Elide.

### <a name="filters"></a>Filters

Filters are JAX-RS or Jersey filter classes. These classes can be used for authentication, logging, or any other type of request filtering you may be required to perform.

Some commonly used servlets & filters are packaged as individual settings.  

#### Codahale / Dropwizard InstrumentedFilter Servlet

Codahale/dropwizard has a servlet and a small set of [administrative filters](https://metrics.dropwizard.io/3.1.0/manual/servlet/) for
exposing Codahale metrics, thread dumps, and system health checks.

These are enabled by default but can be explicitly disabled by overriding `ElideStandaloneSettings`:

```
    /**
     * Whether or not Codahale metrics, healthchecks, thread, ping, and admin servlet
     * should be enabled.
     * @return
     */
    @Override
    boolean enableServiceMonitoring() {
        return false;
    }
```

The admin endpoint is exposed at `/stats`.

New metrics can be exposed through the servlet path `/stats/metrics` by adding them to the static registry found here:
```ElideResourceConfig.getMetricRegistry()```

New health checks can be exposed through the servlet path `/stats/healthcheck` by adding them to the static registry found here:
```ElideResourceConfig.getHealthCheckRegistry()```

### <a name="additional-config"></a>Additional Configuration

You can add additional configuration by specifying the `applicationConfigurator` method. The class (i.e. the `Consumer`) is fully injectable and will take in the root Jersey `ResourceConfig` for your application.

This method accepts a `ResourceConfig` object so you can continue to modify it as necessary.

## <a name="moredetail"></a>Looking for More?

For a more detailed example containing information about using security and additional features, see our [blog example](https://github.com/DennisMcWherter/elide-example-blog-kotlin).
