# Overview

Alpha version of full text search for Elide.  

## Requirements

This store leverages [Hibernate Search](https://hibernate.org/search/) which requires Hibernate 5.4+.

## Usage

`SearchDataStore` wraps another fully featured store and supports full text search on fields that are indexed using Hibernate Search.
If the query cannot be answered by the `SearchDataStore`, it delegates the query to the underlying (wrapped) data store.

### Annotate Your Entity 

Use Hibernate Search annotations to describe how your entities are indexed and stored in Lucene or Elasticsearch.
Some of the annotations (like `AnalyzerDef`) can be defined once at the package level if desired.

```java
@Entity
@Include
@Indexed
@AnalyzerDef(name = "case_sensitive",
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class)
)
@AnalyzerDef(name = "case_insensitive",
        tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = LowerCaseFilterFactory.class)
        }
)
public class Item {
    @Id
    private long id;

    @Fields({
            @Field(name = "name", index = Index.YES,
                    analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "case_insensitive")),
            @Field(name = "sortName", analyze = Analyze.NO, store = Store.NO, index = Index.YES)
    })
    @SortableField(forField = "sortName")
    private String name;

    @Field(index = Index.YES, analyze = Analyze.YES,
            store = Store.NO, analyzer = @Analyzer(definition = "case_insensitive"))
    private String description;

    @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
    @DateBridge(resolution = Resolution.MINUTE, encoding = EncodingType.STRING)
    @SortableField
    private Date modifiedDate;

    private BigDecimal price;
}
```

### Wrap Your DataStore

```java
/* Create your ORM based data store */
DataStore store = ...

/* Wrap it with a SearchDataStore */
EntityManagerFactory emf = Persistence.createEntityManagerFactory("MyPersistenceUnitName");

boolean indexOnStartup = true; //Create a fresh index when the server starts
searchStore = new SearchDataStore(store, emf, indexOnStartup);

/* Configure Elide with your store */
ElideSettings = new ElideSettingsBuidler(searchStore).build();
```

### Indexing your Data
You can index data either by:

1.  When the `SearchDataStore` is initalized, indicate (by setting `indexOnStartup` to true) that it should build a complete index.
2.  Issuing created, updated, and delete requests against your Elide service.
3.  Using an out of band process using Hibernate Search APIs.

## Caveats

1.  Only text fields (String) are supported/tested. Other data types (dates, numbers, etc) have not been tested.
2.  Only IN, NOT, INFIX, and PREFIX operators (and their case insensitive equivalents) are supported.
3.  INFIX is implemented as tokenized PREFIX. A search for '*foo*' will match 'bar foobar' but not 'bar barfoo'.
