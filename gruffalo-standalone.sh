#!/bin/sh

mvn dependency:build-classpath -Dmdep.outputFile=classpath -q
CLASSPATH=target/classes:`cat classpath`
java -cp $CLASSPATH -Xmx2024m -Xms2024m com.outbrain.gruffalo.StandaloneGruffaloServer "$@"
