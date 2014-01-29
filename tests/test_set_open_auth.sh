#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source "$D/library.sh"

[[ -n $TEST_REMOTE_MAC ]] || die "Please set TEST_REMOTE_MAC"
[[ -n $TEST_NETWORK_SSID ]] || die "Please set TEST_NETWORK_SSID"

hciconfig hci0 up
init_test_env

CONNECT_CMD=01

write_attr $COMMAND_ATTR_UUID $CONNECT_CMD
state=$(read_attr $STATUS_ATTR_UUID)

[[ $state == "INITIALIZING" ]] || die "Expected 'INITIALIZING' state"

write_attr $SSID_ATTR_UUID "$(echo $TEST_NETWORK_SSID | $D/unstringify)"
write_attr $PSK_ATTR_UUID "$(echo $TEST_NETWORK_PSK | $D/unstringify)"

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "INITIALIZING" ]] || die "Expected 'INITIALIZING' state"

write_attr $AUTH_ATTR_UUID "$(echo 'OPEN' | $D/unstringify)"

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "INITIALIZING" ]] || die "Expected 'INITIALIZING' state"
write_attr $PSK_ATTR_UUID "00"

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "INITIALIZED" ]] || die "Expected 'INITIALIZED' state"

success
