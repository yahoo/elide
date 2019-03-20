# Change Log
## 4.4.0
**Features**
 * Issue#763 Support for filtering & sorting on computed attributes

## 4.3.3
**Fixes**
 * Issue#744 Better error handling for mismatched method in Lifecycle and additional test

**Features**
 * Let custom Elide wrapper process JsonPatchExtensionException

## 4.3.2
**Fixes**
 * Issue#754

## 4.3.1
**Fixes**
 * Issue#758

**Features**
 * New method in EntityDictionary to bind a dependency injection injector function.

## 4.3.0
**Fixes**
 * Issue#733

**Features**
 * New elide-example-models package
 * New elide-test-helpers package
 * Use SecurityContext as default User object

## 4.2.14
**Features**
 * Added [Codahale InstrumentedFilter](https://metrics.dropwizard.io/3.1.0/manual/servlet/) & corresponding metrics, threads, admin servlets as a setting option for Elide Standalone.

**Fixes**
 * replaced jcabi-mysql-maven-plugin with H2 for testing
 * Upgrade Failsafe to 2.22.1 in order to run Hibernate 5 tests.  Fixed test failure.

## 4.2.13
**Features**
 * Add FilterPredicate sub-classes for each operation type

**Fixes**
 * Upgrade jackson databind to 2.9.8

## 4.2.12
**Fixes**
 * Issue#730
 * Issue#729

## 4.2.11
**Features**
 * Add annotation FilterExpressPath to provide paths for FilterExpressionChecks

## 4.2.10
**Fixes**
 * Upgrade Jetty Server library to address security alerts
 * Issue#703
 * Fix Import Order

## 4.2.9
**Fixes**
 * Fixed IT tests that were not running.
 * Fixed setting private attributes that are inherited.
 * Upgrade Jackson databind library to address security alerts

## 4.2.8
**Fixes**
 * Issue#696
 * Issue#707

## 4.2.7
**Features**
 * Add support for asterisk life cycle hooks (hooks that invoke for all fields in a model).

**Fixes**
 * Add support for multiple classloaders when using CoerceUtils ([Issue #689](https://github.com/yahoo/elide/issues/689))
 * Issue#691
 * Issue#644

**Features**
 * Both JPA Field (new) and Property (4.2.6 and earlier) Access are now supported.

## 4.2.6
**Fixes**
 * Fix NPE serializing Dates

## 4.2.5
**Features**
 * ISO8601 and epoch dates can be toggled in Elide Settings

**Fixes**
 * Fix NPE in HibernateEntityManagerStore
 * Performance enhancement for DataSerializer and MapConverter

## 4.2.4
**Fixes**
 * Fixed issues when running and building on Windows OS

## 4.2.3
**Features**
 * Add `CustomErrorException` and `ErrorObjects` to support custom error objects
 * Allow user to configure to return error objects
 * Update `ElideStandalone` to allow users to programmatically manipulate the `ServletContextHandler`.

**Fixes**
 * Fixed bug in GraphQL when multiple root documents are present in the same payload.  The flush between the documents
   did not correctly handle newly created/deleted objects.
 * Fixed broken graphql link in README.md
 * Fixed elide standalone instructions.
 * Fixed hashcode and equals for some test models

## 4.2.2
**Fixes**
 * Resolve hibernate proxy class for relationship

## 4.2.1
**Fixes**
 * Fixed #640
 * Log runtime exception as error

**Features**
 * Added "fetch joins" for to-one relationships to improve HQL performance and limit N+1 queries.

## 4.2.0
**Features**
 * Upgraded hibernate 5 datastore to latest version (5.2.15)

**Fixes**
 * Fixed bug where create-time pre-security hooks were running before any values were set.

## 4.1.0
**Fixes**
 * Performance enhancements including caching the `Class.getSimpleName`.
 * Fixed bug where updatePreSecurity lifecycle hook was being called for object creation.  This will no longer be true.  This changes the behavior of life cycle hooks (reason for minor version bump).

**Features**
 * Added the ability to register functions (outside entity classes) for lifecycle hook callbacks.

## 4.0.2
**Fixes**
 * Add support for retrieving values from java `Map` types. These are still modeled as lists of key/value pairs.
 * Log GraphQL query bodies. Private information or anything which is not intended to be logged should be passed as a variable as variables values are not logged.
 * Handle the `Transaction not closed` error on aborted response.

## 4.0.1
**Fixes**
 * Change `PersistentResourceFetcher` constructor visibility to public in order to allow this class instantiation outside of the elide-graphql.

## 4.0.0

See: 4.0-beta-5

## 4.0-beta-5
**Fixes**
 * Ignore non-entity types if present in the hibernate class metadata in the hibernate stores. This can legitimately occur when tools like envers are used.

**Features**
 * Support GraphQL batch requests.

## 4.0-beta-4
**Fixes**
 * Ignore provided-- but null-- operation names and variables in GraphQL requests.
 * Add additional logging around exception handling.
 * Don't swallow generic Exception in Elide. Log it and bubble it up to caller.
 * Fix a bug where null filter expressions were possible if no filter was passed in by the user, but permission filters existed.
 * Fix support for handling GraphQL variables.
 * Support java.util.Date types as new built-in primitive. Expects datetime as epoch millis.
 * Fixed issue with supporting variables in mutations.
 * Allow for arbitrary in-transaction identifiers for upserts (treated as unique identifier for current tx only).
 * Ensure GraphQLEndpoint returns GraphQL spec-compliant response.

**Features**
 * Handle ConstraintViolationException's by extracting the first constraint validation failure.
 * Include GraphQL in Elide standalone by default with ability to remove it via dependency management.
 * Upgrade to the latest graphql-java version: 6.0.

## 4.0-beta-3
**Fixes**
 * Updated MIT attribution for portions of MutableGraphQLInputObjectType
 * getRelation (single) call filters in-memory to avoid collision on multiple objects being created in the same transaction.

**Features**
 * ChangeSpec is now passed to OnUpdate life cycle hooks (allowing the hooks to see the before & after change to a given field).

## 4.0-beta-2
**Fixes**
 * Root collection loads now push down security filter predicates.
 * Avoid throwing exceptions that must be handled by the containing application, instead throw exceptions that will be handled directly within Elide.
 * Restore OnCreatePreSecurity lifecycle hook to occur after fields are populated.

**Features**
 * Added UPDATE operation for GraphQL.

## 4.0-beta-1
**Features**
 * Elide now supports GraphQL (as well as JSON-API).  This feature is in beta.  Read the [docs](elide.io) for specifics.  Until the artifact moves to stable,
   we may change the semantics of the GraphQL API through a minor Elide version release.
 * The semantics of `CreationPermission` have changed and can now apply towards fields as well as entities.  `UpdatePermission` is never
   checked for newly created objects.
 * The semantics of `SharePermission` have changed.  `SharePermission` can no longer have an expression defined.  It either denies permission
   or exactly matches `ReadPermission`.
 * RSQL queries that compare strings are now case-insensitive. There is not currently a way to make
   case sensitive RSQL queries, however the RSQL spec does not provide this either.
   Fixes #387

**Fixes**
 * Updated PreSecurity lifecycle hooks to run prior to inline checks like they should.

**Misc**
 * All deprecated functions from Elide 3.0 have been removed.
 * `FilterPredicates` have been restructure to share a common `Path` with other Elide code.

## 3.2.0
**Features**
 * Updated interface to beta standalone application. Plans to finalize this before Elide 4.0 release.

**Fixes**
 * Rollback relationship handling change.
 * Handle ForbiddenAccess only for denied Include, instead of filtering to empty set.

## 3.1.4
**Fixes**
 * Instead of ForbiddenAccess for denied Include, filter to empty set.
 * Generate error when parsing permission expression fails.

## 3.1.3
 * Add support for @Any relationships through a @MappedInterface

## 3.1.2
**Features**
 * Add Elide standalone application library

**Fixes**
 * Fix for issue #508
 * Fix for issue #521
 * Fix blog example
 * Properly handle proxy beans in HQL Builder

## 3.1.1
**Fixes**
 * Fix id extraction from multiplex transaction.

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
