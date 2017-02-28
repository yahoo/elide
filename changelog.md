#Change Log

## 3.0
**Features**
* Revised datastore interface 
    * Removed hibernate-isms
    * Made key-value persistence easier to support
* Changed lifecycle hook model
* Updated audit logger interface
* Removed all deprecated features, e.g.
    * SecurityMode
    * `any` and `all` permission syntax
    * Required use of `ElideSettingsBuilder`
    * Removed `PersistenceStore` from Hibernate 5 datastore
* Made `InMemoryDataStore` the reference datastore implementation

**Fixes**
* Close transactions properly
* Updated all dependencies
