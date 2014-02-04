#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

CFG=$(readlink -f $D/../device_config)

if ! mount|grep -q '/dev/nanda'; then
	sudo mount -t vfat /dev/nanda /mnt
fi

if ! mount|grep -q '/dev/nanda .* /mnt type vfat .*'; then
	die "The device /dev/nanda is not mounted at the expected location: /mnt"
fi

cd $D/.. ; pwd

cp -iv /mnt/script.bin $CFG/script.bin.old.`date +%Y%m%d%H%M%S`

new_fex_bin=$CFG/script.bin.new.`date +%Y%m%d%H%M%S`
fex2bin -v $CFG/script.fex $new_fex_bin

sudo cp -iv $new_fex_bin /mnt/script.bin
sudo chmod -v 0755 /mnt/script.bin

sudo umount /mnt
