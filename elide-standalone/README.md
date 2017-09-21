Table of Contents
=================

  * [Overview](#overview)
  * [Who is this for?](#whofor)
  * [Getting Started](#gettingstarted)
  * [Usage](#usage)
    * [Settings Overview](#settings-overview)
    * [Settings Class](#settings-class)
    * [Filters](#filters)
    * [Additional Configuration](#additional-config)
  * [More Detailed Examples](#moredetail)

## <a name="overview"></a>Overview

The Elide standalone application is a configurable web server using Elide. While Elide is typically a pluggable **middleware** framework, we have constructed a flexible and complete service to allow you to get started quickly.

The Elide standalone application takes an opinionated stance on its technology stack (i.e. jersey/jetty), but provides many opportunities for users to configure the behavior of their application. To use the Elide standalone application, there are only a few steps:
  1. Build a jar containing `elide-standalone`, your models, security checks, and additional application configuration.
  1. Configure _Elide standalone_ using the `elide-settings.properties` file
  1. Start your web service:
     * `$ java -jar YOUR_APP.jar` (for fully built app)
     * `$ java -cp elide-standalone.jar:YOUR_MODEL_AND_SECURITY.jar com.yahoo.elide.standalone.Main` (for application)

To include `elide-standalone` into your project, add the single dependency:
```xml
<dependency>
  <groupId>com.yahoo.elide</groupId>
  <artifact>elide-standalone</artifact>
  <version>3.1.2</version>
</dependency>
```

Similarly, if you would prefer to build a full application (recommended) rather than using the application jar, then build a shaded jar. An example in maven is as follows:

```xml
<build>
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <transformers>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                            <mainClass>com.yahoo.elide.standalone.Main</mainClass>
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
```

## <a name="whofor"></a>Who is this for?

The Elide standalone application is for all new and existing users of Elide. This is the **fastest way to setup an Elide web service** and we have provided several avenues of customization for Elide standalone. However, if you need even more flexibility in your application than what is provided, then you should consider using the Elide __middleware__ directly.

## <a name="gettingstarted"></a>Getting Started

A simple example of setting up an Elide service without security.

### Setup a Database (MySQL)

In our example, we will suggest your create a MySQL database called `elide` that is accessible to the user `elide` with password `elide123`.

> **NOTE:** For the _quickest_ setup possible, enable `demoMode=true` in your `elide-settings.properties` to use the in-memory datastore instead of MySQL and Hibernate. This datastore does not currently support all Elide functionality (i.e. sorting, pagination, filtering), but it does support other basic functionality so you can see Elide in action.

### Create the Model

```java
package com.yourcompany.elide.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include(rootLevel = true)
class Post {
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
            <version>3.1.2</version>
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
                            <mainClass>com.yahoo.elide.standalone.Main</mainClass>
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

### Configure Elide Standalone

```properties
# Web server port
port=8080

# Package containing models
modelPackage=com.yourcompany.elide.models

# Load hibernate config
hibernate5Config=./settings/hibernate.cfg.xml

# Root path of the JSON-API API
jsonApiPathSpec=/api/v1/*
```

### Run Your Service

Using the provided `hibernate.cfg.xml`, you can now run your service (as long as `elide-settings.properties` is in your CWD or in `$CWD/settings/elide-settings.properties`):

```
$ java -cp bin/elide-standalone.jar:example-models-1.0-SNAPSHOT.jar com.yahoo.elide.standalone.Main
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

Using Elide standalone out of box is intended to require minimal effort. For persistence (i.e. `demoMode=false`) you will minimally need a Hibernate5-compatible database (i.e. MySQL), a `Settings` class, and your JPA-annotated data models.

### <a name="settings-overview"></a>Settings Overview

There are a variety of configurable options for Elide standalone. The application will _attempt_ to read this from eihter `./elide-settings.properties` or `./settings/elide-settings.properties` on disk or `/elide-settings.properties` in your classpath. If you store your settings file elsewhere, please provide a system property to the JVM: `-DelideSettings=/path/to/your/settings`. The following options are availble to be set in your settings file:

<table>
<thead>
<tr><th>Setting</th><th>Default</th><th>Description</th></tr>
</thead>
<tr>
  <td>port</td>
  <td>8080</td>
  <td>Port for web server to listen</td>
</tr>
<tr>
  <td>modelPackage</td>
  <td>com.yourcompany.elide.models</td>
  <td>Package containing models</td>
  </tr>
<tr>
  <td>settingsClass</td>
  <td>com.yourcompany.elide.security.Settings</td>
  <td><a href="#settings-class">Programmatic service information</a></td>
</tr>
<tr>
  <td>filters</td>
  <td>null</td>
  <td>Comma-separated list of web service filters</td>
</tr>
<tr>
  <td>additionalApplicationConfiguration</td>
  <td>null</td>
  <td>Extra configuration for servlet</td>
</tr>
<tr>
  <td>hibernate5Config*</td>
  <td>./settings/hibernate.cfg.xml</td>
  <td>Hibernate config</td>
</tr>
<tr>
  <td>demoMode*</td>
  <td>false</td>
  <td>Enable/disable demo mode</td>
</tr>
</table>

__* Unused if custom `ElideSettings` object is provided__

### <a name="settings-class"></a>Settings Class

The `Settings` class is the one that provides all the necessary code-related information to Elide standalone for proper setup. This class is **not required** for anyone expecting to use Elide standalone; defaults can be used for all the gathered settings. If this is not provided, all checks names resolve to fully qualified class names of the checks themselves (so you can still even leverage security).

It supports the following methods:

```java
public Map<String, Class<? extends Check>> getCheckMappings();
public ElideSettings getElideSettings();
public DefaultOpaqueUserFunction getUserExtractionFunction();
```

which should be derived from their respective providers: [CheckMappingsProvider](./src/main/java/com/yahoo/elide/standalone/interfaces/CheckMappingsProvider.java), [ElideSettingsProvider](./src/main/java/com/yahoo/elide/standalone/interfaces/ElideSettingsProvider.java), [UserExtractionFunctionProvider](./src/main/java/com/yahoo/elide/standalone/interfaces/UserExtractionFunctionProvider.java).  

  * `getElideSettings()` This method produces a fully configured `ElideSettings` object to use.
  * `getCheckMappings()` This method provides all security rule check mappings.
  * `getUserExtractionFunction()` This method provides the mechanism for extracting a user from a java `SecurityContext`.

If the `getElideSettings()` method is present, then it is used in place of `getCheckMappings()`. Implementing the latter, however, provides an easier initial setup if the application defaults work well for you. The latter exists for greater configuration flexibility (i.e. custom filter dialects, permission executors, datastores, etc.). 

The `getUserExtractionFunction()` method is always used. If none is provided, a default one is provided for you. The default implementation merely extracts the `Principal` object from the request's `SecurityContext`.

This class is **fully injectable**. That provides some useful basic functionality. Namely, you can inject a list of all your entities in your package by simply adding:

```java
@Inject @Named("elideAllModels") Set<Class> entities;
```

Similarly, you can inject the hk2 `ServiceLocator` if you wish to use injection throughout your application.

### <a name="filters"></a>Filters

Filters are JAX-RS or Jersey filter classes. These classes can be used for authentication, logging, or any other type of request filtering you may be required to perform.

### <a name="additional-config"></a>Additional Configuration

You can add additional configuration by specifying the `additionalApplicationConfiguration` class. This class is fully injectable and supports the following methods:

```java
public void configure(ResourceConfig);
```

This method accepts a `ResourceConfig` object so you can continue to modify it as necessary.

## <a name="moredetail"></a>Looking for More?

For a more detailed example containing information about using security and additional features, see our [blog example](https://github.com/DennisMcWherter/elide-example-blog-kotlin).