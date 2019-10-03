web: java -jar elide-example/elide-blog-example/target/elide-blog-example.jar

release: java -jar elide-example/elide-blog-example/target/dependency/liquibase.jar --changeLogFile=elide-example/elide-blog-example/src/main/resources/db/changelog/changelog.xml --url=$JDBC_DATABASE_URL --classpath=elide-example/elide-blog-example/target/dependency/postgres.jar update
