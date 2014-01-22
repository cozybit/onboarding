BTMAC_SERVER := 00:02:72:C8:78:AD
BTMAC_CLIENT := 60:D8:19:B0:E1:45

BLUEZ_INCLUDES := ~/src/bluez-5.12/lib

all: simplescan l2cap-server l2cap-client advertise mgmt-api

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

advertise: advertise.o
	gcc -o $@ $^ -lbluetooth

advertise.o: advertise.c
	gcc -o $@ -c $^ -DBTMAC_SERVER=\"$(BTMAC_SERVER)\" -DBTMAC_CLIENT=\"$(BTMAC_CLIENT)\"

mgmt-api: mgmt-api.o
	gcc -o $@ $^ -lbluetooth

mgmt-api.o: mgmt-api.c
	gcc -I $(BLUEZ_INCLUDES) -o $@ -c $^ -DBTMAC_SERVER=\"$(BTMAC_SERVER)\" -DBTMAC_CLIENT=\"$(BTMAC_CLIENT)\"

clean:
	rm -f simplescan *.o l2cap-server l2cap-client mgmt-api advertise
