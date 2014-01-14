#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/l2cap.h>

int main(int argc, char **argv)
{
    struct sockaddr_l2 addr = { 0 };
    struct sockaddr_l2 loc_addr = { 0 };
    int s, status;
    char *message = "hello!";

    // allocate a socket
    s = socket(AF_BLUETOOTH, SOCK_SEQPACKET, BTPROTO_L2CAP);

    // bind socket client bt adapter
    loc_addr.l2_family = AF_BLUETOOTH;
    str2ba( BTMAC_CLIENT, &addr.l2_bdaddr );
    bind(s, (struct sockaddr *)&loc_addr, sizeof(loc_addr));

    // set the connection parameters (who to connect to)
    addr.l2_family = AF_BLUETOOTH;
    addr.l2_psm = htobs(0x1001);
    str2ba( BTMAC_SERVER, &addr.l2_bdaddr );

    // connect to server
    status = connect(s, (struct sockaddr *)&addr, sizeof(addr));

    // send a message
    if( status == 0 ) {
        status = write(s, "hello jason!", 6);
    }

    if( status < 0 ) perror("uh oh");

    close(s);
}
