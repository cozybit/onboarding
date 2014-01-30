#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source "$D/library.sh"

[[ -n $TEST_REMOTE_MAC ]] || die "Please set TEST_REMOTE_MAC"
[[ -n $TEST_NETWORK_SSID ]] || die "Please set TEST_NETWORK_SSID"
[[ -n $TEST_NETWORK_PSK ]] || die "Please set TEST_NETWORK_PSK"

hciconfig hci0 up
init_test_env

CONNECT_CMD=01

data=$(read_attr $VENDORID_ATTR_UUID)
[[ $data == "toastmaster" ]] || die "Expected 'toastmaster' as vendor id"

data=$(read_attr $DEVICEID_ATTR_UUID)
[[ $data == "00:02:72:C8:7C:A1" ]] || die "Expected '00:02:72:C8:7C:A1' as device id"

success
