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
  1. Configure ElideStandalone by implementing the ElideStandaloneSettings interface.
  1. Build an uber jar containing `elide-standalone`, your models, security checks, and additional application configuration.
  1. Start your web service:
     * `$ java -jar YOUR_APP.jar`


## <a name="whofor"></a>Who is this for?

The Elide standalone application is for all new and existing users of Elide. This is the **fastest way to setup an Elide web service** and we have provided several avenues of customization for Elide standalone. However, if you need even more flexibility in your application than what is provided, then you should consider using the Elide __middleware__ directly.

## <a name="gettingstarted"></a>Getting Started

To include `elide-standalone` into your project, add the single dependency:	
```xml	
<dependency>	
  <groupId>com.yahoo.elide</groupId>	
  <artifactId>elide-standalone</artifactId>	
  <version>LATEST</version>	
</dependency>	
```

A complete example of using Elide standalone to setup a simple service can be found [here](https://elide.io/pages/guide/01-start.html).

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
