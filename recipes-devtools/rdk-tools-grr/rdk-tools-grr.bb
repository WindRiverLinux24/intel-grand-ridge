DESCRIPTION = "Intel Runtime Development Tools"
HOMEPAGE = "https://www.intel.com"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-or-later;md5=fed54355545ffd980b814dab4a3b312c"

SRC_URI = "${RDK_TOOLS_SOURCE} \
           file://rdk-tools.sh \
           file://rdk-grr-remove-IS_ENABLED-CONFIG_PLDMFW.patch \
           file://rdk-grr-add-missing-unused-meson-option.patch \
           file://rdk-grr-change-to-for-KBUILD_EXTRA_SYMBOLS.patch \
           file://rdk-grr-replace-obsolete-interface-u64_stats_fetch_b.patch \
           file://rdk-grr-klm-cpk-ice_main.c-remove-redundant-pci_enab.patch\
           file://rdk-grr-klm-cpk-ice_txrx.c-include-xdp.h.patch \
           file://rdk-grr-klm-cpk-ice_devlink.c-remove-devlink_info_dr.patch \
           file://rdk-grr-klm-cpk-ice_devlink.c-remove-devlink_set_fea.patch \
           file://rdk-grr-replace-vma-vm_flags-direct-modifications.patch \
           file://rdk-grr-remove-THIS_MODULE-from-class_create.patch \
           file://rdk-grr-klm-cpk-ae-drop-Werror.patch \
           file://rdk-grr-i3c_master_unregister-has-been-changed-to-vo.patch \
           file://rdk-grr-remove-adf_enable_aer-and-adf_disable_aer.patch \
          "

# RDK user space and kernel module source packages
# Define this if the files exist.  Usually done in template feature/grandridge-rdk.
# For example: RDK_TOOLS_SOURCE ?= "file://rdk_user_src_grr.tgz file://rdk_klm_src_grr.tgz"
RDK_TOOLS_SOURCE ??= ""

COMPATIBLE_MACHINE = "null"

inherit module meson pkgconfig

# Currently supported version
RDK_TOOLS_VERSION ?= "240104"

S = "${WORKDIR}/rdk"
PV = "${RDK_TOOLS_VERSION}"

DEPENDS = "virtual/kernel libnl libpcap openssl rsync-native thrift meson-native lttng-ust lttng-tools"
RDEPENDS_${PN} += "lttng-ust lttng-tools"

#RDK Tools installed path
RDK_INSTALL_DIR ?= "/opt/intel/rdk-tools"

export KSRC = "${STAGING_KERNEL_BUILDDIR}"
export SDKTARGETSYSROOT = "${STAGING_DIR_TARGET}"
export OECORE_NATIVE_SYSROOT = "${STAGING_DIR_NATIVE}"

# GRR RDK delivery 11 is only compatible with lttng v2.12, not v2.13
# So disable lttng support here.
export RDK_LTTNG_ENABLE = "false"

export KCFLAGS = "-fmacro-prefix-map=${S}= -fdebug-prefix-map=${S}= "

TARGET_CC_ARCH += "${LDFLAGS}"
EXTRA_OEMAKE = "V=1"

do_configure[noexec] = "1"

do_compile () {

	# RDK meson.build is not compatible with meson-wrapper changed in oe-core
	# commit 87d4f6d176f2 ("meson: provide relocation script and native/cross wrappers also for meson-native").
	# So remove meson-wrapper in RDK build and use the "real" meson.
	rm -f ${STAGING_DIR_NATIVE}/usr/bin/meson
	ln -s ${STAGING_DIR_NATIVE}/usr/bin/meson.real ${STAGING_DIR_NATIVE}/usr/bin/meson

	cd ${S}
	oe_runmake modules
	oe_runmake cpk-ae-lib
	oe_runmake netd-lib
	oe_runmake qat_lib
	oe_runmake ies_api_install
	oe_runmake -j1 nura
	oe_runmake cli
}

do_install () {
	oe_runmake -C ${S} install

	install -d ${D}${RDK_INSTALL_DIR}
	cp -r ${S}/install/*  ${D}${RDK_INSTALL_DIR}

	install -d ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/staging/intel-rdk
	install -m 0644  ${D}${RDK_INSTALL_DIR}/drivers/*.ko ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/staging/intel-rdk/

	install -d ${D}${nonarch_base_libdir}/firmware
	install -D -m 0644 ${D}${RDK_INSTALL_DIR}/drivers/qat_300xx.bin  ${D}${nonarch_base_libdir}/firmware/qat_300xx.bin
	install -D -m 0644 ${D}${RDK_INSTALL_DIR}/drivers/qat_300xx_mmp.bin ${D}${nonarch_base_libdir}/firmware/qat_300xx_mmp.bin

	install -D -m 0644 ${S}/install/lib/firmware/intel/ice_sw/ddp/ice_sw.pkg ${D}${nonarch_base_libdir}/firmware/intel/ice_sw/ddp/ice_sw.pkg
	install -D -m 0644 ${S}/install/lib/firmware/intel/eth56g/phy-fw.bin ${D}${nonarch_base_libdir}/firmware/intel/eth56g/phy-fw.bin

	sed -i 's#prefix=.*#prefix=${prefix}#' ${D}${RDK_INSTALL_DIR}/lib/pkgconfig/*.pc

	rm -rf ${D}/etc

	install -d ${D}${sysconfdir}/profile.d/
	install -m 0755 ${WORKDIR}/rdk-tools.sh ${D}${sysconfdir}/profile.d/
}

SYSROOT_DIRS += "/opt"

FILES:${PN}-dev = "${RDK_INSTALL_DIR}/lib/libies*.so"
FILES:${PN}-staticdev = "${RDK_INSTALL_DIR}/lib/libies*.a"
FILES:${PN} += "${RDK_INSTALL_DIR} \
                /usr/lib/firmware \
                ${sysconfdir}/profile.d/rdk-tools.sh \
               "
KERNEL_MODULE_PROBECONF += "adk_netd dlb2 i3c_rdk ice_sw \
                            ice_sw_ae ies intel_vsec \
                            ipsec_inline oobmsm_rdk \
                            intel_qat qat_300xx qat_c3xxx qat_c4xxx \
                           "

# The following kernel drivers will not be autoloaded during kernel boot.
# User can use "modprobe" to load drivers which they will use.
module_conf_adk_netd = "blacklist adk_netd"
module_conf_dlb2 = "blacklist dlb2"
module_conf_i3c_rdk = "blacklist i3c_rdk"
module_conf_ice_sw = "blacklist ice_sw"
module_conf_ice_sw_ae = "blacklist ice_sw_ae"
module_conf_ies = "blacklist ies"
module_conf_intel_vsec = "blacklist intel_vsec"
module_conf_ipsec_inline = "blacklist ipsec_inline"
module_conf_oobmsm_rdk = "blacklist oobmsm_rdk"
module_conf_intel_qat = "blacklist intel_qat"
module_conf_qat_300xx = "blacklist qat_300xx"
module_conf_qat_c3xxx = "blacklist qat_c3xxx"
module_conf_qat_c4xxx = "blacklist qat_c4xxx"

INSANE_SKIP:${PN} = "already-stripped ldflags"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INSANE_SKIP:${PN} += "buildpaths"
