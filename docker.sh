#!/bin/sh

docker build --no-cache -f Dockerfile -t dependency-cached .
docker build --no-cache -f Dockerfile-cached -t aw-app-ut .