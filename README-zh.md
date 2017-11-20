[![Codacy Badge](https://api.codacy.com/project/badge/Grade/986e1e05fee64702a2377272d664ec6d)](https://www.codacy.com/app/Elide/elide?utm_source=github.com&utm_medium=referral&utm_content=yahoo/elide&utm_campaign=badger)
[![Gitter](https://badges.gitter.im/yahoo/elide.svg)](https://gitter.im/yahoo/elide?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) [![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)

![Elide Logo](http://elide.io/assets/images/elide.svg)

## Elide简介

Elide是一个用[JPA注释模型](http://blog.csdn.net/superdog007/article/details/22651577)轻松搭建[JSON API](http://jsonapi.org.cn/) web服务的Java library。Elide可以迅速搭建和部署高性能web服务，将数据模型以服务的形式开放访问。Elide提供
1. JSON API CRUD操作，用以访问JPA实体对象。任何实体只需要添加注释就能以JSON API的形式开放。
2. **PATCH扩展** - Elide支持[PATCH扩展](http://jsonapi.org.cn/extending/)功能，仅通过一个访问请求就能完成多项建造，修改，和删除实体的任务。
3. **原子请求** - 包括PATCH扩展在内的所有请求皆为原子请求，可以和数据库事务的原子性完全兼容。
4.  **高安全性** - 所有实体操作和实体内部数据的访问具备权限配置，让您的数据安全开放。
5. **灵活日志** - 所有访问日志可以随意配置。
6. **高扩展性** - Elide支持所有CRUD实体访问类型的业务需求，任何数据库只需添加一个JPA对接类或者实现一个自定义的数据存储（`datastore`）就可以变成一个支持JSON API的实体后台。
7. **安全测试** - Elide包含一个[安全测试框架](https://github.com/yahoo/elide-testing-framework)，可以寻找和发现API安全漏洞。
8. **客户端API** - Elide还与一个[JavaScript 客户端 library](https://github.com/yahoo/elide-js) 协同开发。Elide兼容所有[JSON AP 客户端 library](http://jsonapi.org/implementations/)。

## 文档
更多使用指南，请参见[elide.io](http://elide.io/).

## 在Maven中使用Elide
在Maven项目中加入Elide非常简单，只需在你的pom.xml中加入elide-core即可

```xml
<!-- Elide -->
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-core</artifactId>
</dependency>
```

## Elide开发
如果您决定参与开发Elide，请在IDE中添加[Lombok](https://projectlombok.org/)插件。

## 开源许可
The use and distribution terms for this software are covered by the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html).
