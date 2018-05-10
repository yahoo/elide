[![Gitter](https://badges.gitter.im/yahoo/elide.svg)](https://gitter.im/yahoo/elide?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) 
[![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)
[![Coverage Status](https://coveralls.io/repos/github/yahoo/elide/badge.svg?branch=master)](https://coveralls.io/github/yahoo/elide?branch=master)

![Elide Logo](http://elide.io/assets//images/elide-logo.svg)

## Elide简介

Elide是一个互联网和移动端应用数据API搭建平台，只需要一个简单的[JPA注释模型](http://blog.csdn.net/superdog007/article/details/22651577)
就能帮您轻松搭建[GraphQL](https://graphql.org/)和[JSON](http://jsonapi.org.cn/) API web 服务的。

### 标准完善的数据安全保障

Elide提供极具规则性，简单易懂的语法规则，让您轻松搞定实体（entity）的安全访问。

### 移动端性能优化 API

JSON-API和GraphQL能够帮助开发者仅通过一次API接口访问就能获取与某个实体相关的所有数据，而且在移动端传输过程中减少所有不
必要的数据，只返回被请求的数据部分。我们为您精心设计的数据处理系统帮助您解决很多常见的数据应用开发问题，例如：

* 在单次请求操作中实现创建实体数据，同时将其加入现有的实体库
* 创建存储关系复杂的多个相关实体（实体关联图），并将它们并入现有的实体库
* 你可以选择完全删除某个实体数据，也可以选择接触实体关联（并不删除任何数据）
* 完全自由修改实体关联定义
* 修改实体数据的同时还可以访问加入新建实体

Elide还完全支持数据筛选，排序，分页。

### 任何数据写入都可以保证原子性

无论是JSON-API还是GraphQL，Elide支持单个请求中实现多个数据模型的修改操作。创建新的实体，添加实体关系，修改和删除实体保证
事务的原子性（Atomicity）。

### 支持自定义数据持久化机制

你可以用Elide自定义您的持久化方法策略。您可以使用Elide默认支持的ORM或者使用自行开发的数据存储机制。

### 数据模型一览无余

您可以借助自动生成的Swagger文档或者GraphQL数据模型了解，学习，和编写Elide API查询语句。

### 配置轻松自由

您可以按照您的意思轻松配置您需要的数据模型操作，比如添加复杂的二次运算实体数据，数据检查标注（data validation
annotations），或者是自动以的访问请求中间链模块。

## 文档

更多使用指南，请参见[elide.io](http://elide.io/).

## 开发

如果您想给Elide贡献代码，请在IDE中添加[Lombok](https://projectlombok.org/)插件。

## 教程（英文）

[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)

## 开源许可
The use and distribution terms for this software are covered by the Apache License, Version 2.0
(http://www.apache.org/licenses/LICENSE-2.0.html).
