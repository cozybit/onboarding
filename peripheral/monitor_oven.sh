#! /bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

HOST=cozyonboard.appspot.com
#HOST=192.168.1.104:8080

DEV_ID=$1
[[ -n $DEV_ID ]] || die "Usage: monitor_over.sh <devid>"

STATE=-1

while true;
do
	sleep 0.1
	PREV_STATE=${STATE}
	STATE=`cat /sys/class/gpio/gpio4_pc21/value`
	[ ${PREV_STATE} -eq ${STATE} ] && continue
	[ ${STATE} -eq "0" ] && STATUS="off"
	[ ${STATE} -eq "1" ] && STATUS="on"
	wget -O /dev/null "http://$HOST/checkin?vendorid=toastmaster&deviceid=${DEV_ID}&status=${STATUS}"
done
