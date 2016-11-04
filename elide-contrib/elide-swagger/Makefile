.PHONY=all clean test
GSON="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar"
JUNIT="http://search.maven.org/remotecontent?filepath=junit/junit/4.12/junit-4.12.jar"
HAMCREST="http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
all : gson.jar bin
	zsh -c "javac -cp ./src:gson.jar -d bin src/com/**/*.java"

clean :
	-rm -rf bin
	-rm *.jar 2>/dev/null

test : all hamcrest.jar junit.jar test/TestRunner.class
	java -cp bin/:junit.jar:hamcrest.jar test.TestRunner 

gson.jar :
	wget -O gson.jar $(GSON)

hamcrest.jar :
	wget -O hamcrest.jar $(HAMCREST)

junit.jar :
	wget -O junit.jar $(JUNIT)

test/TestRunner.class :
	javac -cp ./bin/:junit.jar:hamcrest.jar -d bin src/test/TestRunner.java src/test/SwaggerTest.java

bin : 
	mkdir bin
