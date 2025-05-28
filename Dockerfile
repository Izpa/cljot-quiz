FROM openjdk:11

WORKDIR /

COPY target/cljot.jar cljot.jar
EXPOSE 3000

CMD java -jar cljot.jar