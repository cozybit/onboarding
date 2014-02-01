#!/bin/bash

export PATH=/usr/sbin:/sbin:$PATH

die () { echo ${*} >&2; exit -1; }

get_wlan() { iw dev | grep 'Interface.*wlan.*' | head -1 | awk '{print $2}'; }

get_dev_id() { 
	local devid=$(hciconfig | grep "BD Address" | head -1 | awk '{print $3}')
	[[ -n ${devid} ]] || die "ERROR: No bluetooth devices found"
	[[ ${#devid} -eq 17 ]] || die "ERROR: Device ID was invalid: '$devid'"
	echo $devid;
}

backup_or_rm_symlink() {
	local target=$1
	if [[ -f $target ]] && [[ ! -h $target ]]; then
		cp -v $target $target.bak
	fi

	if [[ -f $target ]] && [[ -h $target ]]; then
		rm -v $target
	fi
}
