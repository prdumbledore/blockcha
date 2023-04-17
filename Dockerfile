FROM openjdk:11
RUN mkdir /block
ADD . /block
RUN apt-get update && apt-get install dos2unix
RUN cd /block && dos2unix gradlew && ./gradlew JarBuild