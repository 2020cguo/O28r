# Notes about Compiling (if I run into problems)
* Compile with `mvn install -DskipTests`        
* https://github.com/gousiosg/java-callgraph
* input for callgraph: java -jar javacg-0.1-SNAPSHOT-static.jar lib1.jar lib2.jar...
* output for callgraph: M:class1:<method1>(arg_types) (typeofcall)class2:<method2>(arg_types)
* more details in github page
* zookeeper jar files: 
commons-cli-1.5.0.jar
commons-io-2.11.0.jar
jackson-annotations-2.15.2.jar
jackson-core-2.15.2.jar
jackson-databind-2.15.2.jar
javax.servlet-api-3.1.0.jar
jetty-http-9.4.52.v20230823.jar
jetty-io-9.4.52.v20230823.jar
jetty-security-9.4.52.v20230823.jar
jetty-server-9.4.52.v20230823.jar
jetty-servlet-9.4.52.v20230823.jar
jetty-util-9.4.52.v20230823.jar
jetty-util-ajax-9.4.52.v20230823.jar
jline-2.14.6.jar
logback-classic-1.2.10.jar
logback-core-1.2.10.jar
metrics-core-4.1.12.1.jar
metrics-core-4.1.12.1.jar_LICENSE.txt
netty-buffer-4.1.94.Final.jar
netty-codec-4.1.94.Final.jar
netty-common-4.1.94.Final.jar
netty-handler-4.1.94.Final.jar
netty-resolver-4.1.94.Final.jar
netty-transport-4.1.94.Final.jar
netty-transport-classes-epoll-4.1.94.Final.jar
netty-transport-native-epoll-4.1.94.Final.jar
netty-transport-native-unix-common-4.1.94.Final.jar
simpleclient-0.9.0.jar
simpleclient_common-0.9.0.jar
simpleclient_hotspot-0.9.0.jar
simpleclient_servlet-0.9.0.jar
slf4j-api-1.7.30.jar
snappy-java-1.1.10.5.jar
snappy-java-1.1.10.5.jar_LICENSE.txt
zookeeper-3.8.3.jar
zookeeper-jute-3.8.3.jar
zookeeper-prometheus-metrics-3.8.3.jar