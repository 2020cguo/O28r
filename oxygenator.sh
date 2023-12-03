#!/bin/sh

### PATHS ###
# parent directory of all source code, relative to O28r
SOURCE_DIR="zookeeper/"
# parent directory of output files, relative to O28r
OUTPUT_DIR="zookeeper_output"
# parent directory containing jar files, relative to java-callgraph
JAR_FILE_DIR="zookeeper_lib/"

### CALL AND PARSE PMD ###
./pmd-bin-7.0.0-SNAPSHOT/bin/pmd check -d $SOURCE_DIR --rulesets rulesets/java/quickstart.xml > $OUTPUT_DIR/pmd_output.txt
python3 -c "import pmd_parser; pmd_parser.parse('${OUTPUT_DIR}/pmd_output.txt', '${OUTPUT_DIR}/parser_output')"

### CALL JAVAPARSER ###
cd ../javaparser-maven
export PATH=$PATH:apache-maven-3.9.5/bin
mvn clean install
java -jar target/javaparser-maven-sample-1.0-SNAPSHOT-shaded.jar "../O28r/${OUTPUT_DIR}/parser_output" "../O28r/${SOURCE_DIR}" > ../O28r/${OUTPUT_DIR}/error_methods.txt

### CALL JAVA CALLGRAPH ###
cd ../O28r/java-callgraph
export PATH=$PATH:../apache-maven-3.9.5/bin
mvn install -DskipTests
rm -f "../${OUTPUT_DIR}/callgraph.txt"
find $JAR_FILE_DIR -type f -name "*.jar"|while read fname; do
    java -jar target/javacg-0.1-SNAPSHOT-static.jar $fname >> "../${OUTPUT_DIR}/callgraph.txt"
done

### OUTPUT DEPENDENCY PATHS AND METRICS ###
cd ..
python3 -c "import graph_constructor; graph_constructor.main('${OUTPUT_DIR}/callgraph.txt', '${OUTPUT_DIR}/error_methods.txt')" > ${OUTPUT_DIR}/crit_scores.txt