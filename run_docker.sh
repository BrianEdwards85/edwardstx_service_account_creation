#!/bin/bash

/usr/local/bin/lein uberjar

/usr/bin/docker build -t edwardstx/service-account-sercive .

/usr/bin/docker run -d --restart always -v /etc/service:/etc/service --name service-account-sercive edwardstx/service-account-sercive
