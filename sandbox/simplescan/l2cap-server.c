#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/l2cap.h>

#define L2CAP_CID_ATT 0x4



void hexdump(unsigned char *buffer, unsigned long index, unsigned long width)
{
    unsigned long i, spacer;
    for (i=0;i<index;i++)
    {
        printf("%02x ",buffer[i]);
    }
    for (spacer=index;spacer<width;spacer++)
        printf("    ");
    printf(": ");
    for (i=0;i<index;i++)
    {
        if (buffer[i] < 32) printf(".");
        else printf("%c",buffer[i]);
    }
    printf("\n");
}

#define ATT_OP_ERROR                     0x01
#define ATT_OP_MTU_REQ                   0x02
#define ATT_OP_MTU_RESP                  0x03
#define ATT_OP_FIND_INFO_REQ             0x04
#define ATT_OP_FIND_INFO_RESP            0x05
#define ATT_OP_FIND_BY_TYPE_REQ          0x06
#define ATT_OP_FIND_BY_TYPE_RESP         0x07
#define ATT_OP_READ_BY_TYPE_REQ          0x08
#define ATT_OP_READ_BY_TYPE_RESP         0x09
#define ATT_OP_READ_REQ                  0x0a
#define ATT_OP_READ_RESP                 0x0b
#define ATT_OP_READ_BLOB_REQ             0x0c
#define ATT_OP_READ_BLOB_RESP            0x0d
#define ATT_OP_READ_MULTI_REQ            0x0e
#define ATT_OP_READ_MULTI_RESP           0x0f
#define ATT_OP_READ_BY_GROUP_REQ         0x10
#define ATT_OP_READ_BY_GROUP_RESP        0x11
#define ATT_OP_WRITE_REQ                 0x12
#define ATT_OP_WRITE_RESP                0x13
#define ATT_OP_WRITE_CMD                 0x52
#define ATT_OP_PREP_WRITE_REQ            0x16
#define ATT_OP_PREP_WRITE_RESP           0x17
#define ATT_OP_EXEC_WRITE_REQ            0x18
#define ATT_OP_EXEC_WRITE_RESP           0x19
#define ATT_OP_HANDLE_NOTIFY             0x1b
#define ATT_OP_HANDLE_IND                0x1d
#define ATT_OP_HANDLE_CNF                0x1e
#define ATT_OP_SIGNED_WRITE_CMD          0xd2

int main(int argc, char **argv)
{
    struct sockaddr_l2 loc_addr = { 0 }, rem_addr = { 0 };
    char buf[1024] = { 0 };
    int s, client, bytes_read;
    socklen_t opt = sizeof(rem_addr);

    // allocate socket
    s = socket(AF_BLUETOOTH, SOCK_SEQPACKET, BTPROTO_L2CAP);

    // bind socket to port ATT channel id of the bt adapter identified by its mac
    // bluetooth adapter
    loc_addr.l2_family = AF_BLUETOOTH;
    str2ba(BTMAC_SERVER, &loc_addr.l2_bdaddr );
    loc_addr.l2_cid = htobs(L2CAP_CID_ATT);

    if (bind(s, (struct sockaddr *)&loc_addr, sizeof(loc_addr)) < 0) {
        perror("failed to bind");
    }

    // put socket into listening mode
    listen(s, 1);

    // accept one connection
    client = accept(s, (struct sockaddr *)&rem_addr, &opt);

    ba2str( &rem_addr.l2_bdaddr, buf );
    fprintf(stderr, "accepted connection from %s\n", buf);

    memset(buf, 0, sizeof(buf));

    // read data from the client
    while (1) {
        bytes_read = read(client, buf, sizeof(buf));
        if( bytes_read > 0 ) {
            printf("received:\n");
            hexdump(buf, bytes_read, 40);
            switch (buf[0]) {
                case ATT_OP_MTU_REQ:
                    buf[0] = ATT_OP_MTU_RESP;
                    write(client, buf, bytes_read);
                    break;
                case ATT_OP_READ_BY_GROUP_REQ:
                    buf[0] = ATT_OP_READ_BY_GROUP_RESP;
                    write(client, buf, bytes_read);
                    break;
            }
        }
    }

    // close connection
    close(client);
    close(s);
}
