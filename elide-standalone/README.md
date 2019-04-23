Table of Contents
=================

  * [Overview](#overview)
  * [Who is this for?](#whofor)
  * [Getting Started](#gettingstarted)
  * [Usage](#usage)
    * [Settings Class](#settings-class)
    * [Filters](#filters)
    * [Additional Configuration](#additional-config)
  * [More Detailed Examples](#moredetail)

## <a name="overview"></a>Overview

The Elide standalone application is a configurable web server using Elide. While Elide is typically a pluggable **middleware** framework, we have constructed a flexible and complete service to allow you to get started quickly.

The Elide standalone application takes an opinionated stance on its technology stack (i.e. jersey/jetty), but provides many opportunities for users to configure the behavior of their application. To use the Elide standalone application, there are only a few steps:
  1. Configure ElideStandalone by either implementing the ElideStandaloneSettings interface, or providing basic security configuration.
  1. Build an uber jar containing `elide-standalone`, your models, security checks, and additional application configuration.
  1. Start your web service:
     * `$ java -jar YOUR_APP.jar`

To include `elide-standalone` into your project, add the single dependency:
```xml
<dependency>
  <groupId>com.yahoo.elide</groupId>
  <artifactId>elide-standalone</artifactId>
  <version>4.4.1</version>
</dependency>
```

To actually start your Elide application, add the following to your main method:

```java
public class Main {
  public static void main(String[] args) {
    ElideStandalone elide = new ElideStandalone(new ElideStandaloneSettings() {
        @Override
        public String getModelPackageName() {

            //This needs to be changed to the package where your models live.
            return "your.model.package";
        }

    });

    elide.start();
  }
}
```

## <a name="whofor"></a>Who is this for?

The Elide standalone application is for all new and existing users of Elide. This is the **fastest way to setup an Elide web service** and we have provided several avenues of customization for Elide standalone. However, if you need even more flexibility in your application than what is provided, then you should consider using the Elide __middleware__ directly.

## <a name="gettingstarted"></a>Getting Started

Below we'll walk through a complete example of setting up an Elide service without security.

**If you're interested in seeing a more complete example, check out our [ready-to-run example](https://github.com/DennisMcWherter/elide-example-blog-kotlin).**

### Setup a Database (MySQL)

In our example, we will suggest your create a MySQL database called `elide` that is accessible to the user `elide` with password `elide123`.

### Create the Model

```java
package com.yourcompany.elide.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include(rootLevel = true)
public class Post {
    private long id;
    private String content;
    
    @Id
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @Column(nullable = false)
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}
```

### Build the Models Package

An example `pom.xml` for building the model:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.yourcompany.elide</groupId>
    <artifactId>example-models</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- Elide -->
        <dependency>
            <groupId>com.yahoo.elide</groupId>
            <artifactId>elide-standalone</artifactId>
            <version>4.4.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.7.0</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <transformers>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                            <mainClass>com.yourcompany.elide.app.YourMain</mainClass>
                         </transformer>
                    </transformers>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

then run:

```
$ mvn clean package
```

### Starting Elide

To start Elide, just run the `start()` method somewhere in your main function:

```java
public class YourMain {
  public static void main(String[] args) {
    ElideStandalone elide = new ElideStandalone(new ElideStandaloneSettings() {
        @Override
        public String getModelPackageName() {
            //This needs to be changed to the package where your models live.
            return "your.model.package";
        }
    });

    elide.start();
  }
}
```

### Configure Elide Standalone

While you can provide a user extraction function and checks alone, more advanced configuration is done by implementing the `ElideStandaloneSettings` interface.

### Run Your Service

You can now run your service.

```
$ java -jar YOUR_APP.jar
```

### Query Your Service

#### Create a Post

Run the following curl request:

```
$ curl -X POST \
       -H'Content-Type: application/vnd.api+json' \
       -H'Accept: application/vnd.api+json' \
       --data '{
         "data": {
           "type": "post",
           "id": "0",
           "attributes": {
             "content": "This is my first post. woot."
           }
         }
       }' \
       http://localhost:8080/api/v1/post
```

### Query Your Posts

Run the following curl request:

```
$ curl http://localhost:8080/api/v1/post
```

## <a name="usage"></a>Usage

Using Elide standalone out of box is intended to require minimal effort. For persistence, you will minimally need a JPA compatible database (i.e. MySQL), a `Settings` class, and your JPA-annotated data models.

### <a name="settings-class"></a>Settings Class

ElideStandalone is configured by implementing the ElideStandaloneSettings interface. Please see the ElideStandaloneSettings class for documentation about fields.

Similarly, if you need other metadata across your application, it is important to note that the injector is bound with the following:

```java
@Inject @Named("elideAllModels") Set<Class> entities;
```

Likewise, you can inject the hk2 `ServiceLocator` if you wish to use injection throughout your application.

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
