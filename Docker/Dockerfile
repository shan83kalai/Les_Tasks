FROM amazoncorretto:11-alpine-jdk

ENV APP_HOME=/var/lib/play-scala-kafka

RUN mkdir -p ${APP_HOME}
WORKDIR ${APP_HOME}

#bash is required for Play stage scripts
RUN \
    apk update && \
    apk upgrade && \
    apk add bash

#copy Play stage assembly to app home folder
COPY target/universal/stage/ /var/lib/play-scala-kafka/

#expose default Play port
EXPOSE 9000

#to generate Play secret: sbt playGenerateSecret
ENTRYPOINT bin/play-scala-kafka -Dplay.http.secret.key=kZH9?_rpn?M0Fs9g]Z2E>jkbp]>Ynj78OU=gISE97Yo9Yyz>ofTEcI/HTbZn80Yh
