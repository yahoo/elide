# Elide

> _API's obstinados para aplicaciones móviles y web._

![Elide Logo](../../elide-logo.svg)

[![Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/elide)
[![Build Status](https://cd.screwdriver.cd/pipelines/6103/badge)](https://cd.screwdriver.cd/pipelines/6103)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.paiondata.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.paiondata.elide/elide-core)
[![Coverage Status](https://coveralls.io/repos/github/yahoo/elide/badge.svg?branch=master)](https://coveralls.io/github/yahoo/elide?branch=master)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/alerts)
[![Mentioned in Awesome Java](https://awesome.re/mentioned-badge.svg)](https://github.com/akullpp/awesome-java)
[![Mentioned in Awesome GraphQL](https://awesome.re/mentioned-badge.svg)](https://github.com/chentsulin/awesome-graphql)

*Leer en otros idiomas: [中文](../zh/README.md).*

## Tabla de contenido

- [Antecedentes](#antecedentes)
- [Documentación](#documentación)
- [Instalación](#instalación)
- [Uso](#uso)
- [Seguridad](#seguridad)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

## Antecedentes

[Elide](https://elide.io/) es una biblioteca de Java que permite configurar un servicio web basado en modelos [GraphQL](http://graphql.org) o [JSON API](http://jsonapi.org), con un esfuerzo mínimo. Elide soporta dos variantes de APIs:

1. API's CRUD (crear, leer, actualizar, eliminar (por sus siglas en inglés)). Para leer y manipular modelos.

2. API's analíticas para mediciones de agregación sobre uno o mas atributos del modelo.

Elide soporta un número de características:

### Seguridad se convierte en estándar
Control de acceso a campos y entidades a trvés de una sintaxis de permisos declarativa e intuitiva.

### API's amigables con el móvil (Mobile friendly)

JSON-API y GraphQL permite a desarrolladores solicitar grafos de objetos enteros en una simple petición. Solo los elementos solicitados del modelo de datos son devueltos.
Nuestro enfoque obstinado para mutaciones toma en cuenta escenarios comúnes de aplicaciones como:
* Crear un nuevo objeto y agregarlo a una colección existente en una misma operación.
* Crear un conjunto de objetos relacionados (un subgrafo) y conectarlo a un grafo existente.
* Diferenciar entre borrar un objeto y desasociar un objeto de una relación (sin borrarlo).
* Referenciar un objeto recién creado dentro de otras operaciones de mutación.

Filtrado, ordenado, paginación y búsqueda de texto tienen soporte fuera de la caja.

### Atomicidad para escrituras complejas
Elide soporta múltiples mutaciones de modelos de datos en una sola petición, ya sea en JSON-API o GrapQL. Crear objetos, agregarlos a relaciones, modificar o eliminar, todo junto en una sola petición atómica.

### Soporte de consulta analítica
Elide soporta consultas analíticas en los modelos creados gracias a su poderosa capa de semántica. API´s de Elide trabaja de forma nativa con [Yavin](https://github.com/yahoo/yavin) para visualizar, explorar y reportar en sus datos.

### Introspección de esquema
Explore, entienda y componga consultas en nuestra API Elide a través de documentación generada por Swagger o por el esquema de GraphQL.

### Personalice
Personalice el comportamiento de las operaciones del modelo de datos con atributos computados, anotaciones de validación de datos y solicite hooks de ciclo de vida.

### Almacenamiento agnóstico
Elide es agnóstico a su strategia de persistencia particular. Use un ORM o su propia implementación de almacén de datos.

## Documentación

Mas información acerca de Elide en [elide.io](https://elide.io).

## Instalación

Para probar un ejemplo de servicio de Elide, revise este [Projecto de ejemplo en Spring Boot](https://github.com/yahoo/elide-spring-boot-example).

Alternativamente use [elide-standalone](https://github.com/yahoo/elide/tree/master/elide-standalone) que permite una configuración rápida de una instancia local de Elide ejecutándose de manera embebida en una aplicación Jetty.

## Uso

Los siguientes ejemplos aprovechan elide 5 (ahora en pre-lanzamiento). Para documentación en versiones anteriores/estables, visite [aquí](https://elide.io/pages/guide/v4/01-start.html).

### Para aplicaciones CRUD

La manera más simple de utilizar Elide es aprovechando [JPA](https://es.wikipedia.org/wiki/Java_Persistence_API) para mapear sus modelos Elide a persistentes:

Los modelos deben representar un modelo de dominio en su servicio web:

```java
@Entity
public class Book {

    @Id
    private Integer id;

    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}
```

Agregue anotaciones Elide para exponer sus modelos a un servicio web y definir las políticas de seguridad para su acceso:

```java
@Entity
@Include(rootLevel = true)
@ReadPermission("Everyone")
@CreatePermission("Admin OR Publisher")
@DeletePermission("None")
@UpdatePermission("None")
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}
```

Agregue hooks de ciclo de vida a sus modelos para embeber lógica de negocios personalizada que se ejecuta junto con las operaciones CRUD en su servicio web:

```java
@Entity
@Include(rootLevel = true)
@ReadPermission("Everyone")
@CreatePermission("Admin OR Publisher")
@DeletePermission("None")
@UpdatePermission("None")
@LifeCycleHookBinding(operation = UPDATE, hook = BookCreationHook.class, phase = PRECOMMIT)
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}

public class BookCreationHook implements LifeCycleHook<Book> {
    @Override
    public void execute(LifeCycleHookBinding.Operation operation,
                        LifeCycleHookBinding.TransactionPhase phase,
                        Book book,
                        RequestScope requestScope,
                        Optional<ChangeSpec> changes) {
       //Do something
    }
}

```

Mapee expresiones a funciones de seguridad o predicados que son llevados a la capa de persistencia:

```java
    @SecurityCheck("Admin")
    public static class IsAdminUser extends UserCheck {
        @Override
        public boolean ok(User user) {
            return isUserInRole(user, UserRole.admin);
        }
    }
```

Para exponer y consultar estos modelos, siga los pasos documentados en [primeros pasos](https://elide.io/pages/guide/v5/01-start.html).

Para ejemplos de llamadas de API, revise:
1. [*JSON-API*](https://elide.io/pages/guide/v5/10-jsonapi.html) 
2. [*GraphQL*](https://elide.io/pages/guide/v5/11-graphql.html)

### Para API´s analíticas

Modelos analítcos incluyen tablas, mediciones, dimensiones y uniones que pueden ser creados con una fácil configuración del lenguaje HJSON

```hjson
{
  tables: [
    {
      name: Orders
      table: order_details
      measures: [
        {
          name: orderTotal
          type: DECIMAL
          definition: 'SUM({{order_total}})'
        }
      ]
      dimensions: [
        {
          name: orderId
          type: TEXT
          definition: '{{order_id}}'
        }
      ]
    }
  ]
}
```

Más información en configuración y consulta de modelos analíticos está disponible [aquí](https://elide.io/pages/guide/v5/04-analytics.html).

## Seguridad

La seguridad esta documentada a fondo [aquí](https://elide.io/pages/guide/v5/03-security.html).

## Contribuir
Porfavor revise [el archivo contributing.md](../../Contribuiting.md) para información de como involucrarse. Aceptamos issues, pull requests y preguntas.

Si está contribuyendo a Elide usando un IDE, como IntelliJ, asegurese de instalar la extensión [Lombok](https://projectlombok.org/).

Discusión está en [spectrum](https://spectrum.chat/elide) o creando issues.

## Licencia
Este proyecto esta licenciado bajo los términos de licencia open source [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

Por favor revise [LICENSE](../../LICENSE.txt) para los términos completos.

## Artículos
Video introductorio a Elide

[![Intro to Elide](http://img.youtube.com/vi/WeFzseAKbzs/0.jpg)](http://www.youtube.com/watch?v=WeFzseAKbzs "Intro to Elide")

[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)
