# Overview

Elide's `AggregationDataStore` exposes read-only models that support data analytic queries.  Model attributes represent either metrics(for aggregating, filtering, and sorting) and dimensions (for grouping, filtering, and sorting).  Model relationships also represent dimensions, but the models they link to can be managed by other data stores for write-back use cases.

A set of custom annotations allows each model, metric, and dimension to be mapped to native SQL to provide full control over the mapping between a model and an underlying SQL database. 

The `AggregationDataStore` exposes read-only medadata about every table, metric, and dimension it manages as Elide models for introspection capabilities.  

# Querying

# Model Construction

## Tables

Analytic models are any Elide models that are annotated with:
1. `FromTable` - References a database table or view name.
2. `FromSubquery` - References a native SQL query that is used to populate the model.

The AggregationDataStore will automatically scan and bind any Elide models that contain these annotations.

```java
@Include(rootLevel = true)
@FromTable(name = "playerStats")
public class PlayerStats {
```

```java
@Include(rootLevel = true)
@FromSubquery(sql = "SELECT stats.highScore, stats.player_id, c.name as countryName FROM playerStats AS stats LEFT JOIN countries AS c ON stats.country_id = c.id WHERE stats.overallRating = 'Great'")
public class PlayerStatsView {
```

## Metrics
A Metric is any field that is annotated with `MetricAggregation`.  

Metrics can be parameterized with zero or more arguments that are passed in the client API request.

The `MetricAggregation` annotation is bound to an implementation of a `MetricFunction`.  A MetricFunction can be parameterized with
zero or more arguments that are passed in the client API request.  The MetricFunction is also responsible for constructing a templated SQL expression for the metric.  This template is expanded during SQL query construction with the client provided arguments and the physical name of the table column.

```java
@MetricAggregation(function = SqlMax.class)
private long highScore;
```

The `SqlMax` function could be defined as:

```java
public class SqlMax extends SQLMetricFunction {
    public SqlMax() {
        super("max", "max", "sql max function", "MAX(%s)", Collections.emptySet());
    }
}
```
### Mapping To Physical Columns

Metrics can be mapped to a phsyical column by using the JPA `Column` annotation.  If no such annotation is present, the metric name is used as the mapping.

## Attribute Dimensions

Any model attribute that is not a metric is considered a dimension.  When selected in a query, the requested metrics will be aggregated by this dimension.

### Mapping to Physical Column (Same Table)

Similar to metrics, dimension column physical names are taken either from the JPA `@Column` annotation.

### Mapping to Physical Column (Join Table)

It is possible to map a column in a model to a column in a different table through a join using the `JoinTo` annotation.

The JoinTo annotation takes two arguments:

TBD - this is going to change anyway so will document later.

## Time Dimensions

Date and time attributes can be marked with the `Temporal` annotation.  The `Temporal` annotation exposes to the client a set of supported time grains.  Each grain has a name (DAILY) and a native SQL expression that converts the underlying physical column to that particular time grain.

All temporal dimensions are parameterized by time grain in the GraphQL API - allowing the client to change the grain at query time.

```java
public static final String DAY_FORMAT = "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-dd'), 'yyyy-MM-dd')";
public static final String MONTH_FORMAT = "PARSEDATETIME(FORMATDATETIME(%s, 'yyyy-MM-01'), 'yyyy-MM-dd')";

@Temporal(grains = {
        @TimeGrainDefinition(grain = TimeGrain.DAY, expression = DAY_FORMAT),
        @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = MONTH_FORMAT)
}, timeZone = "UTC")
private Date recordedData;
```

# Metadata Annotations

## Cardinality

The `Cardinality` annotation can mark a dimension attribute with a relative size (small, medium, or large).

```java
@Cardinality(size = CardinalitySize.MEDIUM)
private String rating;
```

## Meta

The `Meta` annotation can provide a name and description for any metric or dimension column.

```java
@MetricAggregation(function = SqlMax.class)
@Meta(longName = "awesome score", description = "very awesome score")
private long highScore;
```

# Caveates

0. You can't group by a relationship attribute.
1. You can't sort by a relationship attribute because you can't group by it.
2. We should not expose relationships we can't traverse 
3. Model identifiers for analytic tables store the row number of a given query.  You cannot query by ID since they are not globally unique.
