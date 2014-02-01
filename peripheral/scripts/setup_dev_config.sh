#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

CFG=$(readlink -f $D/../device_config)

DEVICE_NUMBER=$1
[[ -n $DEVICE_NUMBER ]] || die "Usage: setup_device_config.sh <device_number>"

source $D/library.sh

cd /etc/init; pwd

backup_or_rm_symlink onboarding.conf

ln -f -s -v $CFG/onboarding.conf

echo; cd /etc/init.d; pwd

ls -l onboarding
ln -f -s -v /lib/init/upstart-job onboarding

echo; cd /etc; pwd

ls -l wpa_supplicant.conf
backup_or_rm_symlink onboarding.conf

ln -f -s -v $CFG/wpa_supplicant.conf

echo; cd /etc/network; pwd

ls -l interfaces
backup_or_rm_symlink interfaces

ln -f -s -v $CFG/interfaces.dev$DEVICE_NUMBER interfaces

echo

ls -l /etc/network/interfaces
ls -l /etc/wpa_supplicant.conf
ls -l /etc/init.d/onboarding
ls -l /etc/init/onboarding.conf
