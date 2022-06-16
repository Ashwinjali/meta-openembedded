SUMMARY = "A full-featured SSL VPN solution via tun device."
HOMEPAGE = "https://openvpn.net/"
SECTION = "net"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=b76abd82c14ee01cc34c4ff5e3627b89"
DEPENDS = "lzo openssl iproute2 ${@bb.utils.contains('DISTRO_FEATURES', 'pam', 'libpam', '', d)}"

inherit autotools systemd update-rc.d

SRC_URI = "http://swupdate.openvpn.org/community/releases/${BP}.tar.gz \
           file://0001-configure.ac-eliminate-build-path-from-openvpn-versi.patch \
           file://openvpn \
           file://openvpn@.service \
           file://openvpn-volatile.conf"

UPSTREAM_CHECK_URI = "https://openvpn.net/community-downloads"

SRC_URI[sha256sum] = "08340a389905c84196b6cd750add1bc0fa2d46a1afebfd589c24120946c13e68"

# CVE-2020-7224 and CVE-2020-27569 are for Aviatrix OpenVPN client, not for openvpn.
CVE_CHECK_IGNORE += "CVE-2020-7224 CVE-2020-27569"

SYSTEMD_SERVICE:${PN} += "openvpn@loopback-server.service openvpn@loopback-client.service"
SYSTEMD_AUTO_ENABLE = "disable"

INITSCRIPT_PACKAGES = "${PN}"
INITSCRIPT_NAME:${PN} = "openvpn"
INITSCRIPT_PARAMS:${PN} = "start 10 2 3 4 5 . stop 70 0 1 6 ."

CFLAGS += "-fno-inline"

# I want openvpn to be able to read password from file (hrw)
EXTRA_OECONF += "--enable-iproute2"
EXTRA_OECONF += "${@bb.utils.contains('DISTRO_FEATURES', 'pam', '', '--disable-plugin-auth-pam', d)}"

# Explicitly specify IPROUTE to bypass the configure-time check for /sbin/ip on the host.
EXTRA_OECONF += "IPROUTE=${base_sbindir}/ip"

do_install:append() {
    install -d ${D}/${sysconfdir}/init.d
    install -m 755 ${WORKDIR}/openvpn ${D}/${sysconfdir}/init.d

    install -d ${D}/${sysconfdir}/openvpn
    install -d ${D}/${sysconfdir}/openvpn/sample
    install -m 755 ${S}/sample/sample-config-files/loopback-server  ${D}${sysconfdir}/openvpn/sample/loopback-server.conf
    install -m 755 ${S}/sample/sample-config-files/loopback-client  ${D}${sysconfdir}/openvpn/sample/loopback-client.conf
    install -dm 755 ${D}${sysconfdir}/openvpn/sample/sample-keys
    install -m 644 ${S}/sample/sample-keys/* ${D}${sysconfdir}/openvpn/sample/sample-keys

    if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
        install -d ${D}/${systemd_unitdir}/system
        install -m 644 ${WORKDIR}/openvpn@.service ${D}/${systemd_unitdir}/system
        install -m 644 ${WORKDIR}/openvpn@.service ${D}/${systemd_unitdir}/system/openvpn@loopback-server.service
        install -m 644 ${WORKDIR}/openvpn@.service ${D}/${systemd_unitdir}/system/openvpn@loopback-client.service

        install -d ${D}/${localstatedir}
        install -d ${D}/${localstatedir}/lib
        install -d -m 710 ${D}/${localstatedir}/lib/openvpn

        install -d ${D}${sysconfdir}/tmpfiles.d
        install -m 0644 ${WORKDIR}/openvpn-volatile.conf ${D}${sysconfdir}/tmpfiles.d/openvpn.conf
        sed -i -e 's#@LOCALSTATEDIR@#${localstatedir}#g' ${D}${sysconfdir}/tmpfiles.d/openvpn.conf
    fi
}

PACKAGES =+ " ${PN}-sample "

RRECOMMENDS:${PN} = "kernel-module-tun"

FILES:${PN}-dbg += "${libdir}/openvpn/plugins/.debug"
FILES:${PN} += "${systemd_unitdir}/system/openvpn@.service \
                ${sysconfdir}/tmpfiles.d \
               "
FILES:${PN}-sample += "${systemd_unitdir}/system/openvpn@loopback-server.service \
                       ${systemd_unitdir}/system/openvpn@loopback-client.service \
                       ${sysconfdir}/openvpn/sample/"