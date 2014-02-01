#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

echo '*******************************************************************'
echo "* ONBOARDING DAEMON START: $(date)"
echo '*******************************************************************'

sudo su -c "$D/gpio.sh"
sudo $D/monitor_oven.sh "$(get_dev_id)" &
sudo $D/launch.sh
