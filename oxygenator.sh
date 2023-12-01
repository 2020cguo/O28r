#!/bin/sh
# call pmd
FILE_DIR="test_runs/"
SOURCE_FILE="Zookeeper.java"
OUTPUT_DIR="output_files"
./pmd-bin-7.0.0-SNAPSHOT/bin/pmd check -d $FILE_DIR --rulesets rulesets/java/quickstart.xml > $OUTPUT_DIR/pmd_output.txt
python3 -c "import pmd_parser; pmd_parser.parse('${OUTPUT_DIR}/pmd_output.txt', '${OUTPUT_DIR}/parser_output')"
# call candace's schtuff
cd ../javaparser-maven
export PATH=$PATH:apache-maven-3.9.5/bin
mvn clean install
java -jar target/javaparser-maven-sample-1.0-SNAPSHOT-shaded.jar "../O28r/${FILE_DIR}/${SOURCE_FILE}" "../O28r/${OUTPUT_DIR}/parser_output" > ../O28r/${OUTPUT_DIR}/error_methods.txt
# call java callgraph
cd ../O28r/java-callgraph
export PATH=$PATH:../apache-maven-3.9.5/bin
mvn install -DskipTests
jar -cvf CallGraph.jar ../$FILE_DIR/*
java -jar target/javacg-0.1-SNAPSHOT-static.jar CallGraph.jar > "../${OUTPUT_DIR}/callgraph.txt"
cd ..
python3 -c "import graph_constructor; graph_constructor.main('${OUTPUT_DIR}/callgraph.txt', '${OUTPUT_DIR}/error_methods.txt')"