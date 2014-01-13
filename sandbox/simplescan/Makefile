BTMAC := 00:02:72:C5:EC:2D
simplescan: simplescan.o
	gcc -o $@ $^ -lbluetooth

simplescan.o: simplescan.c
	gcc -o $@ -c $^ -DBTMAC=\"$(BTMAC)\"

clean:
	rm -f simplescan *.o
