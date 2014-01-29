modprobe gpio_sunxi
#echo 1 > /sys/class/gpio/export
#echo 2 > /sys/class/gpio/export
echo 3 > /sys/class/gpio/export 
echo 4 > /sys/class/gpio/export 
echo out > /sys/class/gpio/gpio3_pc19/direction
echo in > /sys/class/gpio/gpio4_pc21/direction 
