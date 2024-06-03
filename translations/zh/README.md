# Elide

> _互联网 & 移动端应用标准化API_

![Elide Logo](../../elide-logo.svg)

[![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.paiondata.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.paiondata.elide/elide-core)
[![Coverage Status](https://coveralls.io/repos/github/yahoo/elide/badge.svg?branch=master)](https://coveralls.io/github/yahoo/elide?branch=master)

## 目录

- [简介](#简介)
- [支持分析查询](#支持分析查询)
- [文档](#文档)
- [安装](#安装)
- [如何使用](#如何使用)
- [数据安全](#数据安全)
- [开发](#开发)
- [开源许可](#开源许可)
- [参考文献](#参考文献)

## 简介

[Elide](https://elide.io/) 是一个 Java 库，可让您轻松搭建以 [JPA注释模型](http://blog.csdn.net/superdog007/article/details/22651577) 驱动的 [GraphQL](https://graphql.org/) 或 [JSON API](http://jsonapi.org.cn/) Web
服务。Elide 支持两种 API 形式。

1. 用于读取和操作模型的 CRUD（创建、读取、更新、删除）API。
2. 用于聚合零个或多个模型属性的度量的 Analytic API。

Elide 支持多种功能：

### 安全性标准

通过声明的方式或者直观的权限语法来控制对字段和实体的访问。

### 适配移动设备的 API

JSON-API 和 GraphQl 使开发人员在通过单次API接口访问就可获取整个对象的实体图，而且在移动端传输过程中过滤所有不
必要的数据，只返回被请求的数据部分。我们为您精心设计的数据处理系统帮助您解决很多常见的数据应用开发问题，例如：

- 在单次请求操作中创建一个新实体数据并将其添加到现有实体库中。
- 创建一组相关的复合对象（子图）并将其连接到现有的实体库中。
- 区分删除对象与从关系中解关联对象(但不删除对象)。
- 修改实体关联定义。
- 在其他变异操作中引用新创建的对象。

过滤、排序、分页和文本搜索都是开箱即用的。

### 支持复杂的原子操作

Elide 支持在 JSON-API 或 GraphQL 中的单个请求中进行多个数据模型更改。创建新的实体，添加实体关系，在单个原子请求中一起修改或删除。

## 支持分析查询

Elide 支持对其强大的语义层构建的模型进行分析查询。Elide APIs 与 [Yavin](https://github.com/yavin-dev/framework) 协同工作，以能够可
视化的探索和报告数据。

### 自省模式

您可以通过生成的 Swagger 文档或 GraphQL 数据模型了解，探索、理解和编写针对 Elide API 的查询。

### 自定义

您可以通过计算属性、数据验证注释和请求生命周期钩子，来自定义数据模型操作的行为。

### 无关存储

Elide 与您的特定持久性策略无关。使用 ORM 或提供您自己的数据存储实现。

## 文档

关于 Elide 的更多信息可以在 [Elide.io](https://elide.io/) 上查找。

## 安装

要尝试 Elide 示例服务，请查看这个 [Spring boot](https://github.com/paion-data/elide-spring-boot-example) 示例项目。

或者，使用 [Elide-standalone](https://github.com/paion-data/elide/tree/master/elide-standalone)，它允许您快速配置嵌入在 Jetty 应用
程序中运行的 Elide 的本地实例。

## 如何使用

### 对于 CRUD APIs

使用 Elide 最简单的方法是利用 [JPA](https://en.wikipedia.org/wiki/Jakarta_Persistence) 将 Elide 模型映射到持久化:

这些模型应该代表你的 Web Service 的领域模型:

```bash
@Entity
public class Book {

    @Id
    private Integer id;

    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}
```

添加 Elide 注释，既可以通过 Web Service 公开你的模型，也可以定义访问的安全策略:

```bash
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

将生命周期挂钩添加到模型中，以嵌入自定义业务逻辑，通过 Web Service 执行内联的 CRUD 操作:

```bash
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

将表达式映射到被推送至持久层的安全方法或预测中:

```bash
@SecurityCheck("Admin")
public static class IsAdminUser extends UserCheck {
    @Override
    public boolean ok(User user) {
        return isUserInRole(user, UserRole.admin);
    }
}
```

要公开和查询这些模型，请遵循[入门指南](https://elide.io/pages/guide/v5/01-start.html)中记录的步骤。

例如 API 的调用，请看:

1. [*JSON-API*](https://elide.io/pages/guide/v5/10-jsonapi.html)
2. [*GraphQL*](https://elide.io/pages/guide/v5/11-graphql.html)

### 对于 Analytic APIs

包括表、安全策略、表的规格和连接在内的 Analytic 模型既可以作为 pojo 创建，也可以通过友好的 HJSON 配置语言创建。

```bash
{
  tables: [
    {
      name: Orders
      table: order_details
      measures: [
        {
          name: orderTotal
          type: DECIMAL
          definition: 'SUM({{$order_total}})'
        }
      ]
      dimensions: [
        {
          name: orderId
          type: TEXT
          definition: '{{$order_id}}'
        }
      ]
    }
  ]
}
```

关于配置或查询 Analytic 模型的更多信息可以在[这里](https://elide.io/pages/guide/v5/04-analytics.html)找到。

## 数据安全

具体的权限访问详情请参见[该文档](https://elide.io/pages/guide/v7/03-security.html).

## 开发

请阅读[开发文档](CONTRIBUTING.md)，了解如何贡献代码。遇到 bug 或问题，我们欢迎您提交 issues 咨询，或者提交 pull requests.

如果您使用 IDE（例如 IntelliJ）进行开发，请在IDE中添加 [Lombok](https://projectlombok.org/) 插件。

社区聊天目前处于[不和谐](https://discord.com/widget?id=869678398241398854&theme=dark)状态。点击[此处](https://discord.com/invite/3vh8ac57cc)加入讨论。

## 开源许可

该项目根据 [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html) 开源许可证条款获得许可。请参阅[许可证](https://github.com/paion-data/elide/blob/master/LICENSE.txt)了解完整条款。

## 参考文献

Elide 视频简介

[![Intro to Elide](http://img.youtube.com/vi/WeFzseAKbzs/0.jpg)](http://www.youtube.com/watch?v=WeFzseAKbzs "Intro to Elide")

[使用 Spring Boot 和 Elide 创建 JSON API REST 服务](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[使用 Spring Boot/Elide Json API 服务器自定义安全权限](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[使用 Spring Boot 和 Elide 保护 JSON API REST 服务](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[在 Spring Boot/Elide JSON API 服务器中创建实体](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[使用 Spring Boot/Elide JSON API 服务器更新和删除](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)
