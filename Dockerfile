## Stage 1 : build with maven builder image with native capabilities
FROM quay.io/quarkus/ubi-quarkus-graalvmce-builder-image:22.3-java17 AS build
COPY --chown=quarkus:quarkus mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/
USER quarkus
WORKDIR /code
RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline
COPY src /code/src
COPY agent /code/agent
ARG QUARKUS_PROFILE
ARG APP_NAME

USER root
RUN echo $(ls -1 /code/src)
RUN chmod 777 /code/agent/config.yaml
# install wget
RUN  microdnf  install -y wget
# install jmx agent
RUN cd /code && \
    wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.19.0/jmx_prometheus_javaagent-0.19.0.jar && \
    curl -o 'opentelemetry-javaagent.jar' -L 'https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar'

RUN chmod 777 /src/java-function-app/opentelemetry-javaagent.jar && \
    cp /src/java-function-app/opentelemetry-javaagent.jar /home/site/wwwroot/opentelemetry-javaagent.jar

# build the application
RUN ./mvnw package -DskipTests=true -Dquarkus.application.name=$APP_NAME -Dquarkus.profile=$QUARKUS_PROFILE

RUN mkdir -p /code/target/jmx && \
    cp /code/agent/config.yaml /code/target/jmx/config.yaml

RUN chmod 777 /code/jmx_prometheus_javaagent-0.19.0.jar && \
    cp /code/jmx_prometheus_javaagent-0.19.0.jar /code/target/jmx/jmx_prometheus_javaagent-0.19.0.jar

FROM registry.access.redhat.com/ubi8/openjdk-17:1.14

ENV LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --from=build /code/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /code/target/quarkus-app/*.jar /deployments/
COPY --from=build /code/target/quarkus-app/app/ /deployments/app/
COPY --from=build /code/target/quarkus-app/quarkus/ /deployments/quarkus/
COPY --from=build /code/target/jmx/ /deployments/

EXPOSE 8080
EXPOSE 12345
USER 185

ARG QUARKUS_PROFILE
ARG APP_NAME

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Dquarkus.application.name=$APP_NAME -Dquarkus.profile=$QUARKUS_PROFILE -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"