#!/bin/sh
# call pmd
FILE_DIR="test_runs/"
./pmd-bin-7.0.0-SNAPSHOT/bin/pmd check -d $FILE_DIR --rulesets rulesets/java/quickstart.xml > test_output.txt
python3 -c "import pmd_parser; pmd_parser.parse('test_output.txt', 'parser_output.txt')"
# call candace's schtuff
cd ../javaparser-maven
export PATH=$PATH:apache-maven-3.9.5/bin
mvn clean install
java -jar target/javaparser-maven-sample-1.0-SNAPSHOT-shaded.jar "../O28r/${FILE_DIR}/Test.java" "../O28r/parser_output.txt" > ../O28r/error_methods.txt
# call java callgraph
cd ../O28r/java-callgraph
export PATH=$PATH:../apache-maven-3.9.5/bin
mvn install -DskipTests
jar -cvf Test.jar ../$FILE_DIR/*
java -jar target/javacg-0.1-SNAPSHOT-static.jar Test.jar > "../callgraph.txt"
cd ..
python3 -c "import graph_constructor; graph_constructor.main('callgraph.txt', 'error_methods.txt')"