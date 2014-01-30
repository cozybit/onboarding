#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))

node_modules=/home/linaro/ble/node_modules

sudo su $D/gpio.sh
sudo $D/monitor_oven.sh &
sudo $D/launch.sh "$node_modules"
