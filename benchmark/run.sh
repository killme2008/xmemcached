#!/usr/bin/env bash

cd "$(dirname "$0")" # script dir

mvn -B clean package

cd ".."
docker-compose up -d
cd "./benchmark"

java -server -Xms1G -Xmx1G -XX:NewSize=256m -XX:MaxNewSize=256m \
  -cp "./memcachedjava251/target/memcachedjava251.jar:./memcachedjava251/lib/*" \
  net.rubyeye.memcached.benchmark.java_memcached.JavaMemCached

java -server -Xms1G -Xmx1G -XX:NewSize=256m -XX:MaxNewSize=256m \
  -cp "./spymemcached25/target/spymemcached25.jar:./spymemcached25/lib/*" \
  net.rubyeye.memcached.benchmark.spymemcached.Spymemcached

java -server -Xms1G -Xmx1G -XX:NewSize=256m -XX:MaxNewSize=256m \
  -cp "./xmemcached1261/target/xmemcached1261.jar:./xmemcached1261/lib/*" \
  net.rubyeye.memcached.benchmark.xmemcached.Xmemcached

cd ".."
docker-compose kill
