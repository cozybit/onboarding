#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))

die () { echo ${*}; exit -1; }

ONBOARD_NODE_MODULES=$1
[[ -n $ONBOARD_NODE_MODULES ]] || die "Usage: launch.sh <node_modules>"

ONBOARD_WLAN_DEVICE=$(iw dev | grep Interface | awk '{print $2}')
ONBOARD_DEVICE_ID=$(hciconfig | grep "BD Address" | head -1 | awk '{print $3}')
ONBOARD_VENDOR_ID="toastmaster"

[[ ${#ONBOARD_DEVICE_ID} -eq 17 ]] \
    || die "Device ID was invalid: $ONBOARD_DEVICE_ID"

exec sudo \
	ONBOARD_NODE_MODULES=$ONBOARD_NODE_MODULES \
	ONBOARD_WLAN_DEVICE=$ONBOARD_WLAN_DEVICE \
	ONBOARD_DEVICE_ID=$ONBOARD_DEVICE_ID \
	ONBOARD_VENDOR_ID=$ONBOARD_VENDOR_ID \
	BLENO_HCI_DEVICE_ID=0 \
	DEBUG=* \
	node onboarding.js
