FROM openjdk:8

ARG VERSION
ENV VERSION $VERSION

ADD distr/wavebeans-$VERSION.tar /opt/
ADD entrypoint.sh /opt/wavebeans-$VERSION/

WORKDIR /opt/wavebeans-$VERSION

ENTRYPOINT /opt/wavebeans-$VERSION/entrypoint.sh