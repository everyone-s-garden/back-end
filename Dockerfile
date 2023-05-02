FROM openjdk:11-jre-slim

MAINTAINER JINKYUM PARK
WORKDIR ./
EXPOSE 80

ARG API_MAFRA_SECRET

ENV BUILD_DIR=./build/libs/*.jar
ENV API_MAFRA_SECRET=$API_MAFRA_SECRET

COPY ${BUILD_DIR} ./garden.jar
ENTRYPOINT ["java", "-jar", "./garden.jar"]