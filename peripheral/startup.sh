#!/bin/bash

die () { echo ${*}; exit -1; }

wlan_dev=$(iw dev|grep Interface|awk '{print $2}')
node_modules=/home/linaro/ble/node_modules
device_id=$(hciconfig | grep "BD Address" | head -1 | awk '{print $3}')
vendor_id="toastmaster"

[[ ${#device_id} -eq 17 ]] || die "Device ID was invalid: $device_id"

sudo su <<EOF
	~/onboarding_demo/peripheral/gpio.sh
	~/onboarding_demo/peripheral/monitor_oven.sh&
	~/onboarding_demo/peripheral/launch.sh \
        "$node_modules" "$wlan_dev" "$device_id" "$vendor_id"
EOF
