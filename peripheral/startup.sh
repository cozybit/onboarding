#!/bin/bash

sudo su <<EOF
	~/onboarding_demo/peripheral/gpio.sh
	~/onboarding_demo/peripheral/monitor_oven.sh&
	~/onboarding_demo/peripheral/launch.sh /home/linaro/ble/node_modules wlan0
EOF
