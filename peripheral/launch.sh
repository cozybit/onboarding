#!/bin/bash


ONBOARD_NODE_MODULES=$1
ONBOARD_WLAN_DEVICE=$2

exec sudo \
	ONBOARD_NODE_MODULES=$ONBOARD_NODE_MODULES \
	ONBOARD_WLAN_DEVICE=$ONBOARD_WLAN_DEVICE \
	BLENO_HCI_DEVICE_ID=0 \
	DEBUG=* \
	node onboarding.js
