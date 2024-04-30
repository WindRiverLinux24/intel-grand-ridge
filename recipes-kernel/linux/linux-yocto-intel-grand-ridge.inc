KMACHINE:intel-grand-ridge = "intel-x86-64"

COMPATIBLE_MACHINE:intel-grand-ridge = "intel-grand-ridge"

do_install:append:intel-grand-ridge() {
    rm -rf ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/crypto
}
