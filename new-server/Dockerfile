FROM registry.cn-hangzhou.aliyuncs.com/acs/maven AS build-env
ENV MY_HOME=/usr/src/app
RUN mkdir -p $MY_HOME
WORKDIR $MY_HOME
ADD pom.xml $MY_HOME
# get all the downloads out of the way
RUN ["/usr/local/bin/mvn-entrypoint.sh","mvn","verify","clean","--fail-never"]
# add source
ADD . $MY_HOME
# run maven install
RUN ["/usr/local/bin/mvn-entrypoint.sh","mvn","install"]

FROM java:8u111-jdk
WORKDIR /app
COPY --from=build-env /usr/src/app/target/  .
ENTRYPOINT ["java","-jar","/app/new-server.jar"]