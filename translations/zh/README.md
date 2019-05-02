# Elide

> _互联网 & 移动端应标准化API_

![Elide Logo](../../elide-logo.svg)

[![Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/elide)
[![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)
[![Coverage Status](https://coveralls.io/repos/github/yahoo/elide/badge.svg?branch=master)](https://coveralls.io/github/yahoo/elide?branch=master)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/alerts)

## 目录

- [简介](#简介)
- [文档](#文档)
- [安装使用](#安装使用)
- [用法介绍](#用法介绍)
- [数据安全](#数据安全)
- [开发](#开发)
- [开源许可](#开源许可)

## 简介

Elide是一个互联网和移动端应用数据API搭建平台，只需要一个简单的[JPA注释模型](http://blog.csdn.net/superdog007/article/details/22651577)
就能帮您轻松搭建[GraphQL](https://graphql.org/)和[JSON](http://jsonapi.org.cn/) API web 服务。 

Elide 功能强大，包括：

### 标准完善的数据安全保障

Elide提供极具规则性，简单易懂的语法规则，让您轻松搞定实体（entity）的安全访问。

### 移动端性能优化 API

JSON-API和GraphQL使得开发者能够通过单次API接口访问获取与某个实体相关的所有数据，而且在移动端传输过程中过滤所有不
必要的数据，只返回被请求的数据部分。我们为您精心设计的数据处理系统帮助您解决很多常见的数据应用开发问题，例如：

* 在单次请求操作中实现创建实体数据，同时将其加入现有的实体库
* 创建存储关系复杂的多个相关实体（实体关联图），并将它们并入现有的实体库
* 你可以选择完全删除某个实体数据，也可以选择解除实体关联（并不删除任何数据）
* 完全自由地修改实体关联定义
* 修改实体数据的同时还可以访问新建的实体

Elide还完全支持数据筛选，排序，分页。

### 任何数据写入都可以保证原子性

无论是JSON-API还是GraphQL，Elide支持单个请求中实现多个数据模型的修改操作。创建新的实体，添加实体关系，修改和删除实体保证
事务的原子性（Atomicity）。

### 支持自定义数据持久化机制

你可以用Elide自定义您的持久化方法策略。您可以使用Elide默认支持的ORM或者使用自行开发的数据存储机制。

### 数据模型一览无余

您可以借助自动生成的Swagger文档或者GraphQL数据模型了解，学习，和编写Elide API查询语句。

### 配置轻松自由

您可以按照您的意愿轻松配置您需要的数据模型操作，比如添加复杂的二次实体数据运算，数据标注检查（data validation
annotations），或者是自动以的访问请求中间链模块。

## 文档

更多使用指南，请参见[elide.io](http://elide.io/).

### 教程（英文）

[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)

## 安装使用

使用Elide最快捷的方法是用 [elide-standalone](https://github.com/yahoo/elide/tree/master/elide-standalone)，在一个嵌入式
Jetty（embedded Jetty）服务器里运行一个单独的 Elide 软件。

## 用法介绍

要使用 Elide，需要创建一个用 JPA 注释过的数据模型（data models），作为你的 web service 的 domain model。

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

下一步是添加 Elide 注释，将你的数据模型在 web service 开放出来，然后配置访问权限：


```java
@Entity
@Include(rootLevel = true)
@ReadPermission("Everyone")
@CreatePermission("Admin OR Publisher")
@DeletePermission("Noone")
@UpdatePermission("Noone")
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}
```

您可以给你的数据模型添加运行插件，以实现单独的业务需求，这主要是通过建立、读取、更新、删除（CRUD）来运行的：

```java
@Entity
@Include(rootLevel = true)
@ReadPermission("Everyone")
@CreatePermission("Admin OR Publisher")
@DeletePermission("Noone"
@UpdatePermission("Noone")
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;

    @OnCreatePreCommit
    public void onCreate(RequestScope scope) {
       //Do something
    }
}
```

您可以将一些表达式转化成访问权限设置，直接控制数据库存储访问：

```java
    public static class IsAdminUser extends UserCheck {
        @Override
        public boolean ok(User user) {
            return isUserInRole(user, UserRole.admin);
        }
    }
```

最后需要将这些设置挂载到数据模型上，方法请参见 [elide-standalone](https://github.com/yahoo/elide/tree/master/elide-standalone):

```java
public class YourMain {
  public static void main(String[] args) {

    ElideStandaloneSettings settings = new ElideStandaloneSettings() {

        @Override
        public String getModelPackageName() {
            //This needs to be changed to the package where your models live.
            return "your.model.package";
        }
    
        @Override
        public Map<String, Class<? extends Check>> getCheckMappings() {
            //Maps expression clauses to your security check functions & predicates
            return new HashMap<String, Class<? extends Check>>() { {
                put("Admin", IsAdminUser.class);
            }
        };
    });

    ElideStandalone elide = new ElideStandalone(settings);

    elide.start();
  }
}
```

更多 API 功能，请详见：

1. [*JSON-API*](http://elide.io/pages/guide/10-jsonapi.html)
2. [*GraphQL*](http://elide.io/pages/guide/11-graphql.html)

## 数据安全

具体的权限访问详情请参见[该文档](http://elide.io/pages/guide/03-security.html).

## 开发

请阅读[开发文档](CONTRIBUTING.md)，了解如何贡献代码。遇到bug或问题，我们欢迎您提交 issues，咨询，或者提交 pull
requests.

如果您想给Elide贡献代码，请在IDE中添加[Lombok](https://projectlombok.org/)插件。

开源社区设在[spectrum](https://spectrum.chat/elide)，您也可以提交 issues。

## 开源许可
The use and distribution terms for this software are covered by the Apache License, Version 2.0
(http://www.apache.org/licenses/LICENSE-2.0.html).
