---
sidebar_position: 2
title: Search Data Store
---

Overview
--------

Search Data Store provides full text search for Elide.

### Requirements

This store leverages [Hibernate Search](https://hibernate.org/search/) which requires Hibernate 6+.

### Usage

`SearchDataStore` wraps another fully featured store and supports full text search on fields that are indexed using
Hibernate Search. If the query cannot be answered by the `SearchDataStore`, it delegates the query to the underlying
(wrapped) data store.

#### Annotating Entity

Use Hibernate Search annotations to describe how your entities are indexed and stored in Lucene or Elasticsearch. Some
of the annotations (like `AnalyzerDef`) can be defined once at the package level if desired.

```java
@Entity
@Include
@Indexed
@Data // Lombok
public class Item {

    @Id
    private long id;

    @FullTextField(
            name = "name",
            searchable = Searchable.YES,
            projectable = Projectable.NO,
            analyzer = "case_insensitive"
    )
    @KeywordField(name = "sortName", sortable = Sortable.YES, projectable = Projectable.NO, searchable = Searchable.YES)
    private String name;

    @FullTextField(searchable = Searchable.YES, projectable = Projectable.NO, analyzer = "case_insensitive")
    private String description;

    @GenericField(searchable = Searchable.YES, projectable = Projectable.NO, sortable = Sortable.YES)
    private Date modifiedDate;

    private BigDecimal price;
}
```

#### (Optional) Defining a Custom Analyzer

The `Item` entity above references a non-standard analyzer - `case_insensitive`.  This analyzer needs to be
programmatically created:

```java
public class MyLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {

    @Override
    public void configure(LuceneAnalysisConfigurationContext ctx) {
        ctx.analyzer("case_insensitive")
                .custom()
                .tokenizer(NGramTokenizerFactory.class)
                .param("minGramSize", "3")
                .param("maxGramSize", "50")
                .tokenFilter(LowerCaseFilterFactory.class);
    }
}
```

and then configured by setting the property `hibernate.search.backend.analysis.configurer` to the new analyzer.

```xml
<persistence xmlns="http://java.sun.com/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
             version="2.0">
    <persistence-unit name="searchDataStoreTest">
        <class>com.paiondata.elide.datastores.search.models.Item</class>

        <properties>
            <property name="hibernate.search.backend.analysis.configurer" value="class:com.paiondata.elide.datastores.search.MyLuceneAnalysisConfigurer"/>
            <property name="hibernate.search.backend.directory.type" value="local-heap"/>
            ...
        </properties>
    </persistence-unit>
</persistence>
```

#### Wrapping DataStore

```java
/* Create your JPA data store */
DataStore store = ...

/* Wrap it with a SearchDataStore */
EntityManagerFactory emf = ...

boolean indexOnStartup = true; //Create a fresh index when the server starts
searchStore = new SearchDataStore(store, emf, indexOnStartup);

/* Configure Elide with your store */
ElideSettings = new ElideSettingsBuidler(searchStore).build();
```

#### Indexing Data

We can index data either by:

1. When the `SearchDataStore` is initialized, indicate (by setting `indexOnStartup` to `true`) that the search store
   should build a complete index.
2. Issuing created, updated, and delete requests against our Elide service.
3. Using an out of band process using Hibernate Search APIs.

### Caveats

#### Data Type Support

Only text fields (String) are supported/tested. Other data types (dates, numbers, etc) have not been tested. Embedded
index support has not been implemented.

#### Filter Operators

Only INFIX, and PREFIX filter operators (and their case insensitive equivalents) are supported.  Note that hibernate
search only indexes and analyzes fields as either case sensitive or not case-sensitive - so a given field will only
support the INFIX/PREFIX filter operator that matches how the field was indexed.

All other filter operators are passed to the underlying wrapped JPA store.

#### Analyzer Assumptions

##### Index Analysis

To implement correct behavior for Elide's INFIX and PREFIX operators, the search store assumes an ngram (non-edge)
tokenizer is used. This allows white spaces and punctuation to be included in the index.

If the client provides a filter predicate with a term which is smaller or larger than the min/max ngram sizes
respectively, it will not be found in the index.

The search store can be configured to return a 400 error to the client in those scenarios by passing the minimum and
maximum ngram size to the constructor of the `SearchDataStore`.  The sizes are global and apply to all Elide entities
managed by the store instance:

```java
new SearchDataStore(jpaStore, emf, true, 3, 50);
```

##### Search Term Analysis

Elide creates a Hibernate Search `SimpleQueryString` for each predicate.  It first escapes white space and punctuation
in any user provided input (to match Elide's default behavior when not using the `SearchDataStore`).  The resulting
single token is used to construct a prefix query.

##### Sorting and Pagination

When using the INFIX operator, sorting and pagination are pushed to down Lucene/ElasticSearch. When using the PREFIX
operator, they are performed in-memory in the Elide service.

Elide constructs a Prefix query, which together with an ngram index fully implements the INFIX operator.  However, the
ngram analyzer adds ngrams to the index that do not start on word boundaries.  For the prefix operator, the search
store first performs the lucene filter and then filters again in-memory to return the correct set of matching terms.

In this instance, because filtering is performed partially in memory, Elide also sorts and paginates in memory as well.
