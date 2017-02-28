#Change Log

## 3.0
**Features**
* Revised datastore interface 
    * Removed hibernate-isms
    * Made key-value persistence easier to support
* New pre/post hook model
* Updated audit logger interface
* Removed all deprecated features
    * SecurityMode
    * `any` and `all` permission syntax
    * Must use Elide.Builder
* InMemoryDataStore is now the reference datastore implementation

**Fixes**
* Close transactions properly
