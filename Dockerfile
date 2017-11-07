FROM openjdk:8
RUN mkdir /etc/serivce 
COPY ./target/service-account-sercive.jar /srv/service-account-sercive.jar
WORKDIR /srv

ENTRYPOINT /usr/bin/java -Dconfig="/etc/service/service-account-sercive.edn" -jar /srv/service-account-sercive.jar


