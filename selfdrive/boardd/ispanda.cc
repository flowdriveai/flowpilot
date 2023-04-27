#include <iostream>
#include <assert.h>
#include <libusb-1.0/libusb.h>

int main(int argc, char **argv) {
    libusb_context *context;
    libusb_device_handle *handle;
    libusb_device *device;
    struct libusb_device_descriptor desc;
    int fd;

    assert((argc > 1) && (sscanf(argv[1], "%d", &fd) == 1));
    
    #ifdef LIBUSB_OPTION_WEAK_AUTHORITY // present in special proot android build.
        libusb_set_option(NULL, LIBUSB_OPTION_WEAK_AUTHORITY);
    #endif
    assert(!libusb_init(&context));
    assert(!libusb_wrap_sys_device(context, (intptr_t) fd, &handle));
    device = libusb_get_device(handle);
    assert(!libusb_get_device_descriptor(device, &desc));

    if (desc.idVendor == 0xbbaa && (desc.idProduct == 0xddcc || desc.idProduct == 0xddee)) 
        std::cout << "True" << std::endl;
    else
        std::cout << "False" << std::endl;
    libusb_exit(context);
}