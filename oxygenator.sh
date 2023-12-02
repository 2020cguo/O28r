#!/bin/sh
# call pmd
SOURCE_DIR="cassandra/"     # parent directory of all source code, relative to O28r
# SOURCE_FILE="zookeeper"
OUTPUT_DIR="cassandra_output"       # parent directory of output files, relative to O28r
JAR_FILE_DIR="cassandra_lib/"   # parent directory containing jar files, relative to java-callgraph
./pmd-bin-7.0.0-SNAPSHOT/bin/pmd check -d $SOURCE_DIR --rulesets rulesets/java/quickstart.xml > $OUTPUT_DIR/pmd_output.txt
python3 -c "import pmd_parser; pmd_parser.parse('${OUTPUT_DIR}/pmd_output.txt', '${OUTPUT_DIR}/parser_output')"
# call candace's schtuff
cd ../javaparser-maven
export PATH=$PATH:apache-maven-3.9.5/bin
mvn clean install
java -jar target/javaparser-maven-sample-1.0-SNAPSHOT-shaded.jar "../O28r/${OUTPUT_DIR}/parser_output" "../O28r/${SOURCE_DIR}" > ../O28r/${OUTPUT_DIR}/error_methods.txt
# call java callgraph
cd ../O28r/java-callgraph
export PATH=$PATH:../apache-maven-3.9.5/bin
mvn install -DskipTests
# jar -cvf CallGraph.jar ../$SOURCE_DIR
# java -jar target/javacg-0.1-SNAPSHOT-static.jar CallGraph.jar > "../${OUTPUT_DIR}/callgraph.txt"
JAR_FILES="$(find $JAR_FILE_DIR -type f -name "*.jar") | tr '\n' ' '"
java -jar target/javacg-0.1-SNAPSHOT-static.jar $JAR_FILES > "../${OUTPUT_DIR}/callgraph.txt"
cd ..
python3 -c "import graph_constructor; graph_constructor.main('${OUTPUT_DIR}/callgraph.txt', '${OUTPUT_DIR}/error_methods.txt')"