#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

ONBOARD_WLAN_DEVICE=$(get_wlan)
ONBOARD_DEVICE_ID=$(get_dev_id)
ONBOARD_VENDOR_ID="toastmaster"

sudo hciconfig hci0 down
sudo hciconfig hci0 up

cd $D

LOG=/tmp/onboarding.log

rm -f $LOG
touch $LOG

tail -f $LOG &
tail_pid=$!

sudo \
	ONBOARD_WLAN_DEVICE=$ONBOARD_WLAN_DEVICE \
	ONBOARD_DEVICE_ID=$ONBOARD_DEVICE_ID \
	ONBOARD_VENDOR_ID=$ONBOARD_VENDOR_ID \
	BLENO_HCI_DEVICE_ID=0 \
	NODE_DISABLE_COLORS=1 \
	DEBUG=* \
	node $D/../js/onboarding.js >$LOG 2>&1 &

node_pid=$!

trap "sudo kill $node_pid $tail_pid" EXIT
wait
