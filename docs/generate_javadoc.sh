#!/bin/bash

set -e;

JAVA_HOME=C:/dev/Java/jdk-17.0.2+8;

cp="\
../commons-lang/target/classes;\
../commons-util/target/classes;\
../commons-nullanalysis/target/classes;\
../commons-text/target/classes";

#	-link https://docs.oracle.com/javase/11/docs/api/ \
$JAVA_HOME/bin/javadoc \
	-sourcepath "../lfr-core/src/main/java;../ref4j/src/main/java" \
	-classpath "$cp" \
	-d apidocs \
	-Xdoclint:none \
	-J-Dhttp.proxyHost=localhost \
	-J-Dhttp.proxyPort=999 \
	-J-Dhttps.proxyHost=localhost \
	-J-Dhttps.proxyPort=999 \
	-subpackages \
	de.unkrig.lfr.core \
	de.unkrig.ref4j \
	;
