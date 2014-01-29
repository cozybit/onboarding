#!/bin/bash

set -e
D=$(dirname $(readlink -f ${BASH_SOURCE}))

BASE_UUID="ffffffff-c0c1-ffff-c0c1-20140100000"

SERVICE_UUID="${BASE_UUID}0"

STATUS_ATTR_UUID="${BASE_UUID}1"
SSID_ATTR_UUID="${BASE_UUID}3"
PSK_ATTR_UUID="${BASE_UUID}5"
COMMAND_ATTR_UUID="${BASE_UUID}7"

handle_start=
handle_end=
attr_handles=`tempfile --suffix testdata`

trap "rm -f /tmp/*.testdata" EXIT

init_test_env() {
    local handles=$(gatttool -b $TEST_REMOTE_MAC --primary | grep $SERVICE_UUID)

    handle_start=$(echo $handles | sed 's/attr handle = \(.*\),.*/\1/')
    handle_end=$(echo $handles | sed 's/.*end grp handle = \(.*\) .*[:].*/\1/')

    gatttool -b $TEST_REMOTE_MAC --characteristics $handle_start $handle_end >$attr_handles
}

function die () {
    echo ${*}
    exit -1
}

sleep_countdown() {
    local count=$1
    local msg=$2
    local current=0
    echo -n "$msg"
    while [[ $current -lt $count ]]; do
        sleep 1
        echo -n "."
        current=$(($current + 1))
    done
    echo
}

read_attr() {
    local attr_uuid=$1

    local attr_handle=$(cat $attr_handles | grep $attr_uuid)
    local value_handle=$(echo $attr_handle | sed 's/.*char value handle = \(.*\),.*/\1/')

    echo ">>> Read attr UUID" >&2

    #echo $value_handle
    gatttool -b $TEST_REMOTE_MAC --char-read --handle=$value_handle \
        | sed 's#Characteristic value/descriptor: \(.*\)#\1#' \
        | $D/stringify
}

write_attr() {
    local attr_uuid=$1
    local attr_value=$2

    echo ">>> Write attr UUID: $attr_uuid, value: '$attr_value'" >&2

    attr_handle=$(cat $attr_handles | grep $attr_uuid)
    value_handle=$(echo $attr_handle | sed 's/.*char value handle = \(.*\),.*/\1/')

    gatttool -b $TEST_REMOTE_MAC --char-write-req --handle=$value_handle --value="$attr_value"
}

[[ -n $TEST_REMOTE_MAC ]] || die "Please set TEST_REMOTE_MAC"
[[ -n $TEST_NETWORK_SSID ]] || die "Please set TEST_NETWORK_SSID"
[[ -n $TEST_NETWORK_PSK ]] || die "Please set TEST_NETWORK_PSK"

hciconfig hci0 up
init_test_env

CONNECT_CMD=01

write_attr $COMMAND_ATTR_UUID $CONNECT_CMD
state=$(read_attr $STATUS_ATTR_UUID)

[[ $state == "INITIALIZING" ]] || die "Expected 'INITIALIZING' state"

write_attr $SSID_ATTR_UUID "$(echo $TEST_NETWORK_SSID | $D/unstringify)"
write_attr $PSK_ATTR_UUID "$(echo $TEST_NETWORK_PSK | $D/unstringify)"

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "INITIALIZED" ]] || die "Expected 'INITIALIZED' state"

write_attr $COMMAND_ATTR_UUID $CONNECT_CMD

sleep_countdown 5 "Waiting for connection..."

state=$(read_attr $STATUS_ATTR_UUID)
[[ $state == "CONNECTED" ]] || die "Expected 'CONNECTED' state"

echo ">>>"
echo ">>> SUCCESS"
echo ">>>"
