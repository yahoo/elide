package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.H2Dialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.HiveDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.PrestoDialect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class with static methods to create an instance of all Dialects
 */
public class SQLDialectFactory {

    public SQLDialectFactory(){}

    public static SQLDialect getDefaultDialect(){ return new H2Dialect();}

    public static SQLDialect getH2Dialect(){ return new H2Dialect();}

    public static SQLDialect getHiveDialect(){ return new HiveDialect();}

    public static SQLDialect getPrestoDialect(){ return new PrestoDialect();}

    /**
     * Produce a list of all Dialects
     * @return
     */
    public static Map<String, SQLDialect> getAllDialects(){
        Map<String, SQLDialect> dialects = new HashMap();
        dialects.put("H2",getH2Dialect());
        dialects.put("Hive",getHiveDialect());
        dialects.put("Presto",getPrestoDialect());

        return dialects;
    }
}
