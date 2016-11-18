.PHONY: all clean test tags vim
GSON="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.6.2/gson-2.6.2.jar"
JUNIT="http://search.maven.org/remotecontent?filepath=junit/junit/4.12/junit-4.12.jar"
HAMCREST="http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
all : gson.jar bin
	zsh -c "javac -cp ./src:gson.jar -d bin src/com/**/*.java"

clean :
	-rm -rf bin
	-rm *.jar 2>/dev/null
	-rm tags

test : all hamcrest.jar junit.jar bin/test/SwaggerTest.class
	java -cp .:junit.jar:hamcrest.jar:bin org.junit.runner.JUnitCore test.SwaggerTest  

gson.jar :
	wget -O gson.jar $(GSON)

hamcrest.jar :
	wget -O hamcrest.jar $(HAMCREST)

junit.jar :
	wget -O junit.jar $(JUNIT)

bin : 
	mkdir bin

tags :
	zsh -c "ctags -R ./**/*.java(N)"

vim :
	zsh -c "vim ./**/*.java"

bin/test/SwaggerTest.class : src/test/SwaggerTest.java
	javac -cp hamcrest.jar:junit.jar:./src/:gson.jar src/test/SwaggerTest.java -d bin

