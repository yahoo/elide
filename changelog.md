#Change Log

## 3.0
**Features**
* Promoted `DefaultOpaqueUserFunction` to be a top-level class
* Promoted `Elide.Builder` to be a top-level class `ElideSettingsBuilder` 
* Revised datastore interface 
    * Removed hibernate-isms
    * Made key-value persistence easier to support
* Revised lifecycle hook model
* Revised audit logger interface
* Removed all deprecated features, e.g.
    * SecurityMode
    * `any` and `all` permission syntax
    * Required use of `ElideSettingsBuilder`
    * Removed `PersistenceStore` from Hibernate 5 datastore
* Made `InMemoryDataStore` the reference datastore implementation

**Fixes**
* Close transactions properly
* Updated all dependencies
