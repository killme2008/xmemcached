FROM dexterbt1/kestrel
WORKDIR /usr/local/kestrel/current
CMD ["java","-jar","kestrel_2.9.2-2.4.2-SNAPSHOT.jar","-f","config/development.scala"]
