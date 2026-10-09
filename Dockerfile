FROM ubuntu:latest
LABEL authors="vijit"

ENTRYPOINT ["top", "-b"]