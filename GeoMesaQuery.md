# GeoMesa Indexing and Querying #

## Indices ##

[XZ3 (or Z3), XZ2 (or Z2), and record](http://www.geomesa.org/documentation/1.2.5/user/data_management.html#index-structure) indices are built by default.
XZ3 or XZ2 indices are built for objects with extent, and Z3 or Z2 are used for points.
[Join](http://www.geomesa.org/documentation/1.2.5/user/data_management.html#join-indices) and
[full](http://www.geomesa.org/documentation/1.2.5/user/data_management.html#full-indices) attribute indices are also available.

### Z3 ###

The primary spatio-temporal index (for points) is an index over the Z3 addresses of the geometries.

![img2](https://cloud.githubusercontent.com/assets/11281373/17036137/f76a4028-4f58-11e6-98f1-45e995c1ca15.png)

![accumulo-key](https://cloud.githubusercontent.com/assets/11281373/17036145/fe4acd4a-4f58-11e6-9932-03aaa376410e.png)

The `Row ID` is of variable length: [the first 2 bytes are the epoch if there is no split, and the first 3 bytes are the concatenation of the split number (not shown in the picture above) and the epoch if there is a split](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/Z3IdxStrategy.scala#L142-L147).
The [split number](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L193) is the modulus of the hash of the object and the number of splits.
The epoch is a 16-bit number which nominally represents the [week since the Java epoch](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L36),
but can be configured to hold the [day, month, or year](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/BinnedTime.scala#L14-L39) since the epoch.

For points, X, Y, and Time are fixed at [21, 21, and 20 bits](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/Z3SFC.scala#L17-L19) of precision, respectively.
Time is considered at the resolution of 1 second, and 20 bits is just enough to count the number of seconds in one week.

### Z2 ###

This index is used for performing spatial queries that do not have a temporal constraint on points.
For points, X and Y both have [31 bits](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/Z2SFC.scala#L16-L17) of precision.

### XZ3 and XZ2 ###

XZ3 and XZ2 are the [default indices used to store objects with extent in GeoMesa 1.2.5](https://geomesa.atlassian.net/wiki/display/GEOMESA/GeoMesa+1.2.5+Release+Notes).
An extension of Z-order, [XZ-order](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.73.4894) enlarges regions associated with nodes at the same level within the tree -- the tree implied by the pattern of divisions -- so that they overlap.
This allows items to be associated with only one (Z-order) address,
because the boundaries between neighboring nodes are now rectangles rather than lines,
which resolves the problem of having to multiply-store of objects with extent.
Objects with extent are indexed with a maximum resolution of 36 bits ([12 divisions](https://github.com/locationtech/geomesa/blob/master/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/XZ3Table.scala#L33) into [eighths](https://github.com/locationtech/geomesa/blob/master/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/XZ3SFC.scala#L283-L312)),
while points are indexed with a maximum resolution of 24 bits ([12 divisions](https://github.com/locationtech/geomesa/blob/master/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/XZ2Table.scala#L28) into [quarters](https://github.com/locationtech/geomesa/blob/master/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/XZ2SFC.scala#L263-L285)).

### Record ###

This is an index over the UUIDs of the features.

### Attribute ###

There are two types of attribute indices, join and full.
"Join" indices index the UUID, time, and geometry of an object, while "full" indices index all of its attributes.


## Insert ##

The focus of this section is the Z3 index, the primary spatio-temporal index used.

[`featureStore.addFeatures(featureCollection);`](https://github.com/geomesa/geomesa-tutorials/blob/293cd73c64b55a23f301065e2e50f696ae6a80bc/geomesa-quickstart-accumulo/src/main/java/com/example/geomesa/accumulo/AccumuloQuickStart.java#L212)

1. [AccumuloFeatureStore.addFeatures](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureStore.scala#L30-L52)
2. [AccumuloDataStore.getFeatureWriterAppend](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloDataStore.scala#L398-L417)
3. [AppendAccumuloFeatureWriter constructor](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureWriter.scala#L143-L167)
4. ...
5. AppendAccumuloFeatureWriter.writer which is actually [AccumuloFeatureWriter.writer](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureWriter.scala#L108-L114)
6. [AccumuloFeatureWriter.getTablesAndWriters](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureWriter.scala#L50-L54)
7. [Z3Table.writer](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L64-L108)
8. [Z3Table.getGeomRowKeys](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L191-L203)
9. [Z3Table.zBox](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L205-L219)
10. [Z3Table.zBox](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L221-L226)
11. [Z3Table.getZPrefixes](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L230-L250)

Items 1 through 5 in the list above follow the GeoTools API.

Points are inserted with 62 bits of precision.
For objects with extent, their ranges are repeatedly subdivided until ranges with prefixes of at least 24 bits in length are found, then object is associated with each of those prefixes.

## GeoMesa Query Planning ##

### Purely Spatio-Temporal Queries ###

In this section we analyze the [following query](https://github.com/geomesa/geomesa-tutorials/blob/293cd73c64b55a23f301065e2e50f696ae6a80bc/geomesa-quickstart-accumulo/src/main/java/com/example/geomesa/accumulo/AccumuloQuickStart.java#L254)

```java
FeatureIterator featureItr = featureSource.getFeatures(query).features();
```

assuming that the `SimpleFeature`s that are being looked for are geometries with extent.
We also assume in what follows that the qury is fully spatio-temporal (that it does not implicate UUIDs, attributes, &c).
The result of that line is an iterator of `SimpleFeature`s which are responsive to the query.

Restricting attention to query planning, the line above produces the sequence below

1. [AccumuloFeatureSource.getFeatures](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L87)
2. [AccumuloFeatureSource.getFeatureSource.getFeaturesNoCache](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L106-L107)
3. [AccumuloFeatureCollection constructor](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L117-L139)
4. ...
5. [AccumuloFeatureCollection.reader](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L172-L173)
6. [AccumuloDataStore.getFeatureReader](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloDataStore.scala#L429-L439)
7. [AccumuloFeatureReader.apply](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureReader.scala#L48-L59)
8. [AccumuloFeatureReaderImpl constructor](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureReader.scala#L61-L72)
9. ...
10. [QueryPlanner.runQuery](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryPlanner.scala#L71-L79) 
11. [QueryPlanner.getQueryPlans](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryPlanner.scala#L111-L167)
12. [AccumuloStrategyDecider.getFilterPlan](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/AccumuloStrategyDecider.scala#L18) which is actually [StrategyDecider.getFilterPlan](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-index-api/src/main/scala/org/locationtech/geomesa/index/api/StrategyDecider.scala#L42-L88)
13. [GeoMesaFeatureIndex.getQueryPlan](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-index-api/src/main/scala/org/locationtech/geomesa/index/api/GeoMesaFeatureIndex.scala#L17) which is actually [SpatialFilterStrategy.getQueryPlan](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-index-api/src/main/scala/org/locationtech/geomesa/index/strategies/SpatialFilterStrategy.scala#L18-L19) which is actually [Z3QueryableIndex.getQueryPlan](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/z3/Z3QueryableIndex.scala#L38-L183) which comes via [FilterStrategy](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-index-api/src/main/scala/org/locationtech/geomesa/index/api/FilterPlan.scala#L15-L28)
14. [Z3QueryableIndex.getQueryPlan.toZRanges](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/z3/Z3QueryableIndex.scala#L141-L147)
   - [Called once per component, results are concatenated to every prefix](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/z3/Z3QueryableIndex.scala#L151-L166)
15. [Z3SFC.ranges](https://github.com/locationtech/geomesa/blob/be58332c924eac10260829eb501f8b8c8fb94554/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/Z3SFC.scala#L34-L43)
16. org.locationtech.sfcurve.Z3.zranges which is actually [org.locationtech.sfcurve.ZN.zranges](https://github.com/locationtech/sfcurve/blob/46c668ec9c037a017f5f487d8c00064fc60ee52d/zorder/src/main/scala/org/locationtech/sfcurve/zorder/ZN.scala#L95-L233)

Items 1 through 9 in the list above follow the GeoTools API.

### Other Queries ###

In the case of queries which are not (purely) spatio-temporal, indices other than the Z3 index may be used.
As mentioned earlier, the Z2 index will be used for purely spatial.
In the case of a search over a spatio-temporal box for objects with a particular attribute of a particular value,
GeoMesa uses [cost-based optimization (CBO)](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryStrategyDecider.scala#L34-L52) to determine which index to use.
