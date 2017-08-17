# Change Log
## 3.1.0
**Fixes**
 * Use Entity name when Include is empty.  Cleanup Predicate. 

## 3.0.17
**Features**
Adds support for sorting by relationship (to-one) attributes.
**Misc**
Cleanup equals code style

## 3.0.16
**Misc**
 * Replaced deprecated Hibernate Criteria with JPQL/HQL.

## 3.0.15
**Fixes**
 * Use inverse relation type when updating. 
 
## 3.0.14
**Fixes**
 * Properly handle incorrect relationship field name in Patch request instead of `Entity is null`
 * Properly handle invalid filtering input in HQL filtering
 * Properly handle NOT in filterexpressionchecks
 * Fix parameter order in commit permission check
 
## 3.0.13
**Fixes**
 * Fixing regression in deferred permissions for updates
 
## 3.0.12
**Misc**
 * Cleanup hibernate stores to not care about multi edit transactions
 * Removed dead code from hibernate3 transaction
 * Special read permissions handling of newly created objects in Patch Extension

## 3.0.11
**Fixes**
 * Change `UpdateOnCreate` check to be an `OperationCheck`.

## 3.0.10
**Fixes**
 * Use IdentityHashMap for ObjectEntityCache
 * Miscellaneous cleanup.

## 3.0.9
**Fixes**
 * Fix exception handler to pass verbose log even with unexpected exceptions.
 * Fix life cycle hooks to trigger "general" hooks even when specific field acted upon.
 * Build document list for swagger endpoint at the `/` path.

## 3.0.8
**Features**
 * Add support for FieldSetToNull check.

## 3.0.7
**Features**
 * Add support for sorting by id values
 * Implement functionality for Hibernate5 to support `EntityManager`'s.

**Fixes**
 * Account for inheritance when performing new entity detection during a PATCH Extension request.
 * Upgrade examples to behave properly with latest jersey release.
 * Rethrow `WebApplicationException` exceptions from error response handler.

**Misc**
  * Always setting HQL 'alias' in FilterPredicate Constructor

## 3.0.6
**Misc**
* Cleanup of active permission executor

## 3.0.5
**Fixes**
* Fixed caching of security checks (performance optimization)
* Security fix for inline checks being deferred when used in conjunction with commit checks.
* Security fix to not bypass collection filtering for patch extension requests.

**Features**
* Added UUID type coercion
* Move `InMemoryDataStore` to Elide core. The `InMemoryDataStore` from the `elide-datastore-inmemorydb` package has
    been deprecated and will be removed in Elide 4.0

## 3.0.4
**Fixes**
* Do not save deleted objects even if referenced as an inverse from a relationship.

## 3.0.3
**Fixes**
* Fix HQL for order by clauses preceded by filters.
* Remove extra `DELETE` endpoint from `JsonApiEndpoint` since it's not compliant across all JAX-RS implementations.
* Add support for matching inherited types while type checking.
* Fix tests to automatically set UTC timestamp.
* Fix README information and various examples.

## 3.0.2
**Misc**
* Clean up Elide request handler.

## 3.0.1
**Fixes**
* Updated HQL query aliases for page total calculation in hibernate3 and hibernate5 data stores.

## 3.0.0
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
* Allow filtering on nested to-one relationships

**Fixes**
* Close transactions properly
* Updated all dependencies
* Fixed page totals to honor filter & security permissions evaluated in the DB.
