# Dropwizard-Elide

A [Dropwizard Bundle](http://www.dropwizard.io/0.9.2/docs/manual/core.html#bundles) that allows you to integrate Elide in a [Dropwizard](http://www.dropwizard.io) application. 

# Getting Started

To integrate Elide into your Dropwizard project, include dropwizard-elide into your project's pom.xml:

```xml
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>dropwizard-elide</artifactId>
</dependency>
```

# Example

Inside of the Application class, install the bundle:

```
private final ElideBundle<DropwizardElideConfiguration> elideBundle = new ElideBundle<DropwizardElideConfiguration>(
        Author.class,
        Book.class
) {
    @Override
    public DataSourceFactory getDataSourceFactory(DropwizardElideConfiguration configuration) {
        return configuration.getDataSourceFactory();
    }
};

@Override
public void initialize(Bootstrap<DropwizardElideConfiguration> bootstrap) {
    bootstrap.addBundle(elideBundle);
}

@Override
public void run(DropwizardElideConfiguration config, Environment environment) {
    environment.jersey().register(JsonApiEndpoint.class);
}
```
Find more at https://github.com/yahoo/elide/tree/master/elide-example/dropwizard-elide-example
