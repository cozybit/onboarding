#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
source $D/library.sh

PREBUILT=$D/../prebuilt
CFG=$D/../device_config

source $D/library.sh

sudo apt-get update

sudo apt-cache depends lubuntu-desktop | awk -F ":" '{print $2}' | sed '/^$/d' |  xargs sudo apt-get remove --yes
sudo apt-get remove x11-common --yes
sudo apt-get autoremove --yes

pushd $HOME

tar xvfz $PREBUILT/node_modules.tar.gz

popd

sudo dpkg -i $PREBUILT/cozynodejs_0.10.24-1_armhf.deb

sudo apt-get install --yes $(cat $CFG/packages.list)
