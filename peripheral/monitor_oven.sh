#! /bin/bash

HOST=cozyonboard.appspot.com
#HOST=192.168.1.132:8080

STATE=-1

while true;
do
	sleep 0.1
	PREV_STATE=${STATE}
	STATE=`cat /sys/class/gpio/gpio4_pc21/value`
	[ ${PREV_STATE} -eq ${STATE} ] && continue
	[ ${STATE} -eq "0" ] && STATUS="off"
	[ ${STATE} -eq "1" ] && STATUS="on"
	wget -O /dev/null "http://$HOST/checkin?vendorid=toastmaster&deviceid=00:11:22:22:11:00&status=${STATUS}"
done
