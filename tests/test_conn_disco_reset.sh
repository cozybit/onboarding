#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source "$D/library.sh"

[[ -n $TEST_REMOTE_MAC ]] || die "Please set TEST_REMOTE_MAC"
[[ -n $TEST_NETWORK_SSID ]] || die "Please set TEST_NETWORK_SSID"
[[ -n $TEST_NETWORK_PSK ]] || die "Please set TEST_NETWORK_PSK"

hciconfig hci0 up
init_test_env

CONNECT_CMD=01

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "DISCONNECTED" ]] || die "Expected 'DISCONNECTED' state"

write_attr $AUTH_ATTR_UUID "$(echo 'SECURE' | $D/unstringify)"
write_attr $SSID_ATTR_UUID "$(echo $TEST_NETWORK_SSID | $D/unstringify)"

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "INITIALIZING" ]] || die "Expected 'INITIALIZING' state"

write_attr $PSK_ATTR_UUID "$(echo $TEST_NETWORK_PSK | $D/unstringify)"

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "INITIALIZED" ]] || die "Expected 'INITIALIZED' state"

write_attr $COMMAND_ATTR_UUID $CONNECT_CMD

sleep_countdown 5 "Waiting for connection..."

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "CONNECTED" ]] || die "Expected 'CONNECTED' state"

DISCONNECT_CMD=02
write_attr $COMMAND_ATTR_UUID $DISCONNECT_CMD

sleep_countdown 5 "Waiting for disconnect..."

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "DISCONNECTED" ]] || die "Expected 'DISCONNECTED' state"

RESET_CMD=03
write_attr $COMMAND_ATTR_UUID $RESET_CMD

sleep_countdown 5 "Waiting for reset..."

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "DISCONNECTED" ]] || die "Expected 'DISCONNECTED' state"

success
