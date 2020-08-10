## Validation for Dynamic Config

Validate the config files in local before deployment.

To build and run:
```text
1. mvn clean install
2. Execute Jar File :
   a) java -cp elide-contrib/elide-dynamic-config-helpers/target/elide-dynamic-config-*-jar-with-dependencies.jar com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator --configDir <Path for Config Directory>
   b) java -cp elide-contrib/elide-dynamic-config-helpers/target/elide-dynamic-config-*-jar-with-dependencies.jar com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator --help
```
Expected Configs Directory Structure:
```text
├── CONFIG_DIR/
│   ├── models
│   │   ├── tables
│   │   │   ├── table1.hjson
│   │   │   ├── table2.hjson
│   │   │   ├── ...
│   │   │   ├── tableN.hjson
│   │   ├── security.hjson (optional)
│   │   ├── variables.hjson (optional)
│   ├── db
│   │   ├── sql (optional)
│   │   │   ├── db1.hjson
│   │   │   ├── ...
│   │   │   ├── dbN.hjson
│   │   ├── nonsql (optional)
│   │   │   ├── db1.hjson
│   │   │   ├── ...
│   │   │   ├── dbN.hjson
│   │   ├── variables.hjson (optional)
```
