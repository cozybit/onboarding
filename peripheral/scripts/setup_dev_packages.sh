#!/bin/bash

D=$(dirname $(readlink -f ${BASH_SOURCE}))
PREBUILT=$D/../prebuilt

source $D/library.sh

sudo apt-cache depends lubuntu-desktop | awk -F ":" '{print $2}' | sed '/^$/d' |  xargs sudo apt-get remove --yes
sudo apt-get remove x11-common --yes
sudo apt-get autoremove --yes

pushd $HOME

tar xvfz $PREBUILT/node_modules.tar.gz

popd

sudo dpkg -i $PREBUILT/cozynodejs_0.10.24-1_armhf.deb
