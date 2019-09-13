# Change Log
## 4.5.2
**Fixes**
   * [view commit](https://github.com/yahoo/elide/commit/e6a2ffd8abe950fbe05b6429a3e0a8b13deee3ba) Restore provided on jpa (#932) 
   * [view commit](https://github.com/yahoo/elide/commit/96bac92e39a258901f1336f638c89e9502f5b438) Bump commons-beanutils from 1.9.3 to 1.9.4 
   * [view commit](https://github.com/yahoo/elide/commit/083959f218daa94598c599c9cd7090f1aa81e47e) Fix #934: descriptions and example attributes appearing in swagger with empty string value (#935) 
   * [view commit](https://github.com/yahoo/elide/commit/71b0ce7dd6aadef4b5a949fae922445dade75dea) Refactor IT Tests (ResourceIT and test infrastructure). (#897) 
   * [view commit](https://github.com/yahoo/elide/commit/4043f9ff55a2bff05bd3953e00c05dd05d8b45b6) Enable test-helper tests (#947) 
   * [view commit](https://github.com/yahoo/elide/commit/6254f834414fcb73ffa697c1d4eea1e9f8a5567c) Bump version.jetty from 9.4.19.v20190610 to 9.4.20.v20190813 (#922) 
   * [view commit](https://github.com/yahoo/elide/commit/b0aaf30a83cf218cd01b7f0bc0635c507ea3c581) Update Graphql integration test (#954) 
   * [view commit](https://github.com/yahoo/elide/commit/00c16b618cdffdc47678bcf21cee95fa5459a636) Bump rxjava from 2.2.0 to 2.2.12 (#936) 
   * [view commit](https://github.com/yahoo/elide/commit/459c21678fdde2706eed65fb31e6aebaf9a7fdfe) Bump maven-surefire-plugin from 2.22.1 to 2.22.2 (#928) 
   * [view commit](https://github.com/yahoo/elide/commit/87e260c8cf4a33690c875a0dca50ceca1bcee743) Bump version.jersey from 2.28 to 2.29 (#924) 
   * [view commit](https://github.com/yahoo/elide/commit/6ec1b7a39270a76bd9c60ad86b8323eca1583551) Bump jersey-container-jetty-servlet from RELEASE to 2.29 (#929) 
   * [view commit](https://github.com/yahoo/elide/commit/d642f9279f27dfecad436c6cbc59abd9e20d73d1) Bump maven-jar-plugin from 3.0.2 to 3.1.2 (#927) 
   * [view commit](https://github.com/yahoo/elide/commit/8475b41b7f80849e2bebb1b966ff9c539b6eaf8d) Bump guava from 20.0 to 23.0 (#957) 
   * [view commit](https://github.com/yahoo/elide/commit/f4dddcb021121625013b5498b79bfd608b5230ff) Bump jersey-container-servlet-core from RELEASE to 2.29 (#962) 
   * [view commit](https://github.com/yahoo/elide/commit/19f8547d555fe6255844ab09677fbccc548a5624) Bump slf4j-api from 1.7.26 to 1.7.28 (#961) 
   * [view commit](https://github.com/yahoo/elide/commit/0b026135569856ee84020c249421f172799f1488) Bump build-helper-maven-plugin from 1.12 to 3.0.0 (#960) 
   * [view commit](https://github.com/yahoo/elide/commit/11000a00c621078b4dc33c52e1aa854188ea9afb) Bump ant from 1.8.2 to 1.10.7 (#959) 
   * [view commit](https://github.com/yahoo/elide/commit/aae10ce13bba99ddabd12a0475531f34265166a5) Bump junit.version from 5.5.1 to 5.5.2 (#956) 
   * [view commit](https://github.com/yahoo/elide/commit/1db1a18ab67da8bf18ea5bf54ec47279cee5c7eb) Bump mysql-connector-java from 8.0.16 to 8.0.17 (#955)

## 4.5.1
**Features**
 * Issue #851. Added new method `enableSwagger()` in `ElideStandaloneSettings` class which allows an easier way for binding swagger docs to the given endpoint. Override this method returning the `Map<String, Swagger>` object to bind the swagger docs to string endpoint.
 * Issue #900. Add `@ApiModelProperty` support to `elide-swagger` that makes it possible to customize `description`, `example`, `readOnly` and `required` attributes of object definitions in resulting generates Swagger document.

**Fixes**
 * [Security] Bump jackson databind from 2.9.9 to 2.9.9.3
 * Issue #913. Fix deserialization for optional top-level meta object (#913)
 * Migrated elide-core tests to JUnit 5.

## 4.5.0
**Features**
 * Issue #815.  Added the ability to customize the JPQL generation for a filter operator globally or for a specific entity attribute.
 * Alpha release of a new Elide data store (SearchDataStore) that supports full text search on top of an existing data store.
 * Issue #871. Add ElideSettings property `encodeErrorResponses`, which when enabled will encode error messages to be safe for HTML. This works for both JSONAPI and GraphQL endpoints, with verbose errors or error object settings enabled/disabled.
 * HttpStatusException class now supports the following additional functions: `getErrorResponse(boolean encodeResponse)` and `getVerboseErrorResponse(boolean encodeResponse)`
 * Add `GraphQLErrorSerializer` and `ExecutionResultSerializer` which are added to the `ObjectMapper` provided by the ElideSettings. These are used to parse the GraphQL results, instead of using `ExecutionResult#toSpecification`.

**Fixes**
 * Run vulnerability check during build.  Updated dependencies to fix CVE-2018-1000632, CVE-2017-15708, CVE-2019-10247
 * Upgrade to Hibernate 5.4.1

## 4.4.5
**Fixes**
 * Issue 801
 * Switched to Open JDK 8
 
## 4.4.4
**Fixes**
 * When requesting an ID field whose name is not 'id', an error happens: `No such association id for type xxx`. When the requested field name equals 'id', Elide has been fixed to look for the field with the annotation @Id rather than looking by field name.
 * Support RSQL INFIX, POSTFIX, and PREFIX filters on number types: remove '*' before coercing.

**Features**
* Issue#812 Add support for BigDecimal field in GraphQL. 
* Elide standalone now includes a Hikari connection pool & Hibernate batch fetching by default

## 4.4.3
**Features**
 * When fetching a collection, if there are no filters, sorting, or client specified pagination, the ORM backed data stores will return the proxy object rather than construct a HQL query.  This allows the ORM the opportunity to generate SQL to avoid the N+1 problem.

**Fixes**
 * Fixes bug where EntityManager creation for ElideStandalone was not thread safe.

## 4.4.2
**Fixes**
 * Fix error in lookupEntityClass and add test
 * Restore Flush mechanism for Hibernate but allow for customization.

## 4.4.1
**Features**
 * Switch ElideStandAlone to use JPA DataStore by default
 * Enable support for JPA @MapsId annotation on relationships so that client doesn't have
   to provide a dummy ID to make entity creation work.

**Fixes**
 * Flush once for patch extension
 * ConstraintViolationExceptions are propagated on flush (JPA Transaction)
 * Enable support for JPA @MapsId annotation on relationships so that client doesn't have
   to provide a dummy ID to make entity creation work.
 * Cache all calls to getEntityBinding

## 4.4.0
**Features**
 * Issue#763 Support for filtering & sorting on computed attributes
 * Added [JPA Data Store](https://github.com/yahoo/elide/pull/747)

**Fixes**
 * Throw proper exception on invalid PersistentResource where id=null
 * Issue#744 Elide returns wrong date parsing format in 400 error for non-default DateFormats
 * Enable RSQL filter dialect by default (in addition to the default filter dialect).

## 4.3.3
**Fixes**
 * Issue#744 Better error handling for mismatched method in Lifecycle and additional test
 * Upgraded puppycrawl.tools (checkstyle) dependency to address CVE-2019-9658
 * Issue#766 Outdated MySQL driver in elide-standalone and examples

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
