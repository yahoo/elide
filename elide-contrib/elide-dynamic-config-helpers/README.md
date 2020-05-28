## Validation for Dynamic Config

Validate the config files in local before deployment.

To build and run:
```text
1. mvn clean install
2. Look for Jar File under elide-contrib/elide-dynamic-config-helpers/target directory with name matching "elide-dynamic-config-*-jar-with-dependencies.jar"
3. Execute Jar File :
   a) java -cp <Jar File Name> com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator --configDir <Path for Model Configs Directory>
   b) java -cp <Jar File Name> com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator --help
```
Expected Model Configs Directory Structure:
```text
├── MODEL_CONFIG_DIR/
│   ├── tables
│   │   ├── table1.hjson
│   │   ├── table2.hjson
│   │   ├── ...
│   │   ├── tableN.hjson
│   ├── security.hjson (optional)
│   ├── variables.hjson (optional)
```
