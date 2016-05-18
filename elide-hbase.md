# Proposal: HBase with Elide

### Goal
- To support basic Java persistence functionalities(CRUD) for HBase entities through Elide.
- To bridge with other existed data store types in Elide.

### HBase JPA adaptor
1. Define `HBasePersistence` that implements `javax.persistence.spi.PersistenceProvider` that reads a `persistence.xml` with custom configs(including hbase nodes, ports, namespace)
1. Define `HBaseEntityManagerFactory` that implements `javax.persistence.EntityManagerFactory`
1. (Optional) Define `HBaseEntityManager` that implements `javax.persistence.EntityManager`. It should use the HBase Java Client to perform CRUD operations with HBase.
1. Define `HBaseStore` that implements `com.yahoo.elide.core.DataStore`
1. Define `HBaseTransaction` that implements `com.yahoo.elide.core.DataStoreTransaction`

### Entity beans
In order to support having byte arrays(or any arbitrary primitive types) as column qualifiers and column family names in HBase, some indirection hacks will need to be done with the `@Column` annotation.

The entity bean should contain a map where the key is a String, and value is an Object that represents the actual column family/column qualifier names.

The `name` attribute in `@Column` should look like `"cfPointer1:cqPointer1"`, where `cfPointer1` and `cqPointer1` are keys in the map described above. If keys cannot be found, we consider the name as is.

For example:
If there's a Person HTable, and looks like:  
```
ROW    COLUMN+CELL
\x01   \x02:\x03, timestamp=1431924360459, value=\x04
```

Then the HBase entity bean should look like:
```
@Entity
@Table(name = "person")
@DataStore(cls = HBaseStore.class)
public class Person {
  // This map can be injected, doesn't have to be static
  private final Map<String, Object> columnMap = ImmutableMap.of(
      "cf1", new byte[]{2}, // Column Family
      "cq1", new byte[]{3}  // Column Qualifier
  );

  @Id
  private byte[] rowKey;

  @Column(name = "timestamp")
  private Long timestamp;

  @Column(name = "cf1:cq1")
  private byte[] value;

  @OneToOne/@OneToMany/@ManyToOne/@ManyToMany
  @DataStore(cls = HibernateStore.class)
  public Set<HobbyGroup> getHobbyGroups() { return Collections.emptySet(); }
}
```

A Relational Database entity bean should look like:
```
@Entity
@Table(name = "hobbyGroup")
public class HobbyGroup {
  @Id
  private long id;
  private Set<Person> persons;

  @OneToOne/@OneToMany/@ManyToOne/@ManyToMany
  @DataStore(cls = HBaseStore.class)
  @RowKeyFn(cls = RowKeyComputer.class)
  public Set<Person> getPersons() { return Collections.emptySet(); }

  static class RowKeyComputer implements Function<HobbyGroup, Object> { //could return a FilterScope maybe?
    @Override
    public Object apply(HobbyGroup hobbyGroup) {
      byte[] rowKey = new byte[]{hobbyGroup.getId().byteValue()};
      return rowKey;
    }
  }
}
```

### Bridging DataStores
1. Must use a `MultiplexDataStore`
1. `PersistenceResource` will still be responsible for holding security check logics. `DataStoreTransaction` will have more to do with getting/setting attr, relations, etc. Methods in `DataStoreTransaction` aren't aware of securities at all
1. Have a custom annotation `@DataStore(cls)`. This will be used to annotate relations of a different data store type in an entity bean.
```
@DataStore(cls=HBaseStore.class)
public Collection<HBaseEntity> getHBaseEntities() { return Collections.emptySet(); }
```
1. Add a field `parentTransaction` to `DataStoreTransaction`, so that when using a `MultiplexTransaction`, a detail data store transaction object can get access to its parent(the MultiplexTransaction) and get to its peer(a transaction of a different data store type):
```
class DataStoreTransaction {
  default DataStoreTransaction getParentTransaction() { return null; }
}
```
1. Define `getRelation(Object entity, String relationName)` in `DataStoreTransaction`:
```
class DataStoreTransaction {
  <T> Collection<T> getRelation(Object entity, String relationName) {
    // Default implementation should call the getter method(just like the getValue() in PersistenceResource)
    // if the getter method is annotated with @DataStore, retrieve the transaction for the specified datastore from the parent multiplex transaction,
    // and then use it to loadObjects, with the Criteria/FilterScope computed by @RowKeyFn(or maybe rename it) function.
  }
}
```
1. Define `updateRelation(Object entity, String fieldName, Set<PersistentResource> resourceIdentifiers)` in `DataStoreTransaction`:
```
class DataStoreTransaction {
  boolean setRelation(Object entity, String fieldName, Set<PersistentResource> resourceIdentifiers) {
    // Default implementation should call the setter method(just like the setValue() in PersistenceResource)
    // if the setter method is annotated with @DataStore, retrieve the transaction for the specified datastore from the parent multiplex transaction,
    // and then use it to save(serialize), with the Criteria/FilterScope computed by @RowKeyFn(or maybe rename it) function.
  }
}
```

#### Getting & Setting attributes
Attributes of an entity should be mostly within the same data store. It's not usual that a MySQL entity has an *attribute* that lives in HBase(in those cases, a better data model design would be making that attribute a relation.)

### Filtering HBase results
1. In-memory filter. TBD
1. Leverage HBase index. Filtering based on HBase columns should be represented by Elide FilterScope. The HBaseEntityManager should correctly understand the filters and interpret it with HBase Client.

### Prevent HBase to scan too much
1. Row count limit. Load records and count on the fly. TBD
1. Query timeout. Have a Single Thread Scheduled Executor in the data store that monitors timeout. Define a TimeoutTask that represents the timeout:
  ```
  class DataStore {
    ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    class TimeoutTask implements Runnable, Closeable {
        DataStoreTransaction transaction;

        public TimeoutTask(DataStoreTransaction transaction) {
            this.transaction = transaction;
            timeoutScheduler.schedule(this, timeoutMsec, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            // code to run when timeout happens
            transaction.abort();
            conn.cancelQuery();
        }

        @Override
        public void close() {
            conn = null;
        }
    }
  }

  class DataStoreTransaction {
    DataStoreTransaction() {
          ...
          this.timeoutTask = new TimeoutTask(this);
      }
  }
  ```

### Caveats
1. Known issue with `MultiplexDataStore` and `MultiplexTransaction`. Child transactions in a multiplex transaction can be in inconsistent state (e.g. one transaction is already committed while the other fails, the successed transaction won't be able to rollback the change)
