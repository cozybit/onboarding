#!/bin/bash

[ "`which dot`" == "" ] && { echo "ERROR: dot is NOT installed!!."; exit 1; }

for file in `ls *.dot`; do
    dot -Tps ${file} -o ${file}.ps
done
