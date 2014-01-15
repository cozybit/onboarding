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

/* This does not work on my 
   Linux mazinger 3.2.0-57-generic #87-Ubuntu SMP Tue Nov 12 21:35:10 UTC 2013 x86_64 x86_64 x86_64 GNU/Linux
*/

int mgmt_create(void)
{
        struct sockaddr_hci addr;
        int fd;

        fd = socket(PF_BLUETOOTH, SOCK_RAW | SOCK_CLOEXEC | SOCK_NONBLOCK,
                                                                BTPROTO_HCI);
        if (fd < 0) {
                perror("crap!");
                return -errno;
        }

        memset(&addr, 0, sizeof(addr));
        addr.hci_family = AF_BLUETOOTH;
        addr.hci_dev = HCI_DEV_NONE;
        addr.hci_channel = HCI_CHANNEL_CONTROL;

        if (bind(fd, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
                int err = -errno;
                perror("crap!");
                close(fd);
                return err;
        }

        return fd;
}

int main()
{
    return mgmt_create();
}
