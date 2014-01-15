#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <ctype.h>
#include <sys/ioctl.h>
#include <signal.h>
#include <stdio.h>

#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

int mgmt_create(void)
{
        struct sockaddr_hci addr;
        int fd;

        fd = socket(PF_BLUETOOTH, SOCK_RAW | SOCK_CLOEXEC | SOCK_NONBLOCK,
                                                                BTPROTO_HCI);
        if (fd < 0) {
                perror("crap socket!");
                return -errno;
        }

        memset(&addr, 0, sizeof(addr));
        addr.hci_family = AF_BLUETOOTH;
        addr.hci_dev = HCI_DEV_NONE;
        addr.hci_channel = HCI_CHANNEL_CONTROL;

        if (bind(fd, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
                int err = -errno;
                perror("crap bind!");
                close(fd);
                return err;
        }

        return fd;
}

int main()
{
    return mgmt_create();
}
