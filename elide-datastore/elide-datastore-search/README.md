# Overview

Provides full text search for Elide.  

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
@Include(rootLevel = true)
@Indexed
@AnalyzerDef(name = "case_insensitive",
        tokenizer = @TokenizerDef(factory = NGramTokenizerFactory.class, params = {
            @Parameter(name = "minGramSize", value = "3"),
            @Parameter(name = "maxGramSize", value = "10")
        }),
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

1.  When the `SearchDataStore` is initialized, indicate (by setting `indexOnStartup` to true) that the search store should build a complete index.
2.  Issuing created, updated, and delete requests against your Elide service.
3.  Using an out of band process using Hibernate Search APIs.

## Caveats

### Data Type Support

Only text fields (String) are supported/tested. Other data types (dates, numbers, etc) have not been tested.  Embedded index support has not been implemented.

### Operators

Only INFIX, and PREFIX operators (and their case insensitive equivalents) are supported.

### Analyzer Assumptions

#### Index Analysis

To implement correct behavior for Elide's INFIX and PREFIX operators, the search store assumes an ngram (non-edge) tokenizer is used.  
This allows white spaces and punctuation to be included in the index.  

If the client provides a filter predicate with a term which is smaller or larger than the min/max ngram sizes respectively, it will not be found in the index.
The search store can be configured to return a 400 error to the client in those scenarios by passing the minimum and maximum ngram size to
the constructor of the `SearchDataStore`.  The sizes are global and apply to all Elide entities managed by the store instance:

```java
new SearchDataStore(mockStore, emf, true, 3, 50);
```

#### Search Term Analysis

Elide uses a Lucene `KeywordAnalyzer` to analyze the query predicates in filter expressions.  This allows correct handling of white space and punctuation (to match Elide's default behavior
when not using the `SearchDataStore`.

The resulting single token is then used to construct a Lucene prefix query.

#### Sorting and Pagination

When using the INFIX operator, sorting and pagination are pushed to down Lucene/ElasticSearch. When using the PREFIX operator, they are performed in-memory in the Elide service.

Elide constructs a Lucene Prefix query, which together with an ngram index fully implements the INFIX operator.  However, the ngram analyzer adds ngrams to the index that do not start on word 
boundaries.  For the prefix operator, the search store first performs the lucene filter and then filters again in-memory to return the correct set of matching terms.  
In this instance, because filtering is performed partially in memory, Elide also sorts and paginates in memory as well.
