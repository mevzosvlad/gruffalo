#!/bin/sh

mvn dependency:build-classpath -Dmdep.outputFile=classpath -q
CLASSPATH=target/classes:`cat classpath`
# run com.outbrain.gruffalo.StandaloneGruffaloServer -h to check available args and usage
# change port and cluster before starting
java \
  -cp $CLASSPATH \
  -Djava.net.preferIPv4Stack=true \
  -Xmx2024m \
  -Xms2024m \
  com.outbrain.gruffalo.StandaloneGruffaloServer \
  --port 2003 \
  --cluster node01:2003,node02:2003,node03:2003
