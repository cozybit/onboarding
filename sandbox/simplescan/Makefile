BTMAC_SERVER := 00:02:72:C5:EC:2D
BTMAC_CLIENT := 60:D8:19:B0:E1:45

all: simplescan l2cap-server l2cap-client

simplescan: simplescan.o
	gcc -o $@ $^ -lbluetooth

simplescan.o: simplescan.c
	gcc -o $@ -c $^ -DBTMAC_SERVER=\"$(BTMAC_SERVER)\"

l2cap-server: l2cap-server.o
	gcc -o $@ $^ -lbluetooth

l2cap-server.o: l2cap-server.c
	gcc -o $@ -c $^ -DBTMAC_SERVER=\"$(BTMAC_SERVER)\"

l2cap-client: l2cap-client.o
	gcc -o $@ $^ -lbluetooth

l2cap-client.o: l2cap-client.c
	gcc -o $@ -c $^ -DBTMAC_SERVER=\"$(BTMAC_SERVER)\" -DBTMAC_CLIENT=\"$(BTMAC_CLIENT)\"

clean:
	rm -f simplescan *.o l2cap-server
