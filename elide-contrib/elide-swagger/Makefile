.PHONY=all clean test
GSON="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar"
JUNIT="http://search.maven.org/remotecontent?filepath=junit/junit/4.12/junit-4.12.jar"
HAMCREST="http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
all : gson.jar
	javac -cp ./src:gson.jar src/*.java 

clean :
	-rm ./src/*.class 2>/dev/null
	-rm ./test/*.class 2>/dev/null
	-rm *.jar 2>/dev/null

test : hamcrest.jar junit.jar test/TestRunner.class
	java -cp test TestRunner

gson.jar :
	wget -O gson.jar $(GSON)

hamcrest.jar :
	wget -O hamcrest.jar $(HAMCREST)

junit.jar :
	wget -O junit.jar $(JUNIT)

test/TestRunner.class :
	javac 
