#!/bin/bash

#We don't need this in this implementation
#mysql ad < dropSpatialIndex.sql

# Create everything
mysql ad < createSpatialIndex.sql

# Run Indexer
javac -cp /usr/share/java/mysql-connector-java-5.1.28.jar:/usr/share/java/lucene-core-5.4.0.jar:/usr/share/java/lucene-analyzers-common-5.4.0.jar:/usr/share/java/lucene-queryparser-5.4.0.jar:/usr/share/java/lucene-queries-5.4.0.jar:. Indexer.java
java -cp /usr/share/java/mysql-connector-java-5.1.28.jar:/usr/share/java/lucene-core-5.4.0.jar:/usr/share/java/lucene-analyzers-common-5.4.0.jar:/usr/share/java/lucene-queryparser-5.4.0.jar:/usr/share/java/lucene-queries-5.4.0.jar:. Indexer

# Compile Searcher
javac -cp /usr/share/java/mysql-connector-java-5.1.28.jar:/usr/share/java/lucene-core-5.4.0.jar:/usr/share/java/lucene-analyzers-common-5.4.0.jar:/usr/share/java/lucene-queryparser-5.4.0.jar:/usr/share/java/lucene-queries-5.4.0.jar:. Searcher.java

# Here is a good <<hack>> in order to be able to excecute java without the long required libraries
# so we set the login shell script CLASSPATH variable.
CLASSPATH="/usr/share/java/mysql-connector-java-5.1.28.jar:/usr/share/java/lucene-core-5.4.0.jar:/usr/share/java/lucene-analyzers-common-5.4.0.jar:/usr/share/java/lucene-queryparser-5.4.0.jar:/usr/share/java/lucene-queries-5.4.0.jar:."
export CLASSPATH
exec $SHELL -i
