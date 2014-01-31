#! /bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

HOST=cozyonboard.appspot.com

DEV_ID=$1
[[ -n $DEV_ID ]] || die "Usage: monitor_over.sh <devid>"

STATE=-1

checkin() {
    while true; do
        STATE=`cat /sys/class/gpio/gpio4_pc21/value`
        [ ${STATE} -eq "0" ] && STATUS="off"
        [ ${STATE} -eq "1" ] && STATUS="on"
        wget \
            --timeout=5 \
            -O /dev/null \
            "http://$HOST/checkin?vendorid=toastmaster&deviceid=${DEV_ID}&status=${STATUS}"
        if [[ $? -eq 0 ]]; then
            break;
        fi
        sleep 0.5
    done
}

while true;
do
    sleep 0.1
    PREV_STATE=${STATE}
    STATE=`cat /sys/class/gpio/gpio4_pc21/value`
    [ ${PREV_STATE} -eq ${STATE} ] && continue
    checkin
done

# vim:et:sts:ts=4:sw=4:
