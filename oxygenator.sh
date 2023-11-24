#!/bin/sh
# call pmd
./pmd-bin-7.0.0-SNAPSHOT/bin/pmd check -d test_runs --rulesets rulesets/java/quickstart.xml > test_output.txt
python3 -c "import pmd_parser; pmd_parser.parse('test_output.txt', 'parser_output.txt')"
# call candace's schtuff
java -jar ../javaparser-maven/target/javaparser-maven-sample-1.0-SNAPSHOT-shaded.jar > error_methods.txt
# call java callgraph
# python graph_constructor.py