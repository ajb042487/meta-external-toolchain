SUMMARY = "The GNU Compiler Collection - libgcc"
HOMEPAGE = "http://www.gnu.org/software/gcc/"
SECTION = "devel"
DEPENDS += "virtual/${TARGET_PREFIX}binutils"
PROVIDES += "libgcc-initial"
PV = "${GCC_VERSION}"

inherit external-toolchain

LICENSE = "GPL-3.0-with-GCC-exception"

# libgcc needs libc, but glibc's utilities need libgcc, so short-circuit the
# interdependency here by manually specifying it rather than depending on the
# libc packagedata.
RDEPENDS_${PN} += "${@'${PREFERRED_PROVIDER_virtual/libc}' if '${PREFERRED_PROVIDER_virtual/libc}' else '${TCLIBC}'}"
INSANE_SKIP_${PN} += "build-deps file-rdeps"

# The dynamically loadable files belong to libgcc, since we really don't need the static files
# on the target, moreover linker won't be able to find them there (see original libgcc.bb recipe).
BINV = "${GCC_VERSION}"
FILES_${PN} = "${base_libdir}/libgcc_s.so.*"
FILES_${PN}-dev = "${base_libdir}/libgcc_s.so \
                   ${libdir}/${EXTERNAL_TARGET_SYS}/${BINV}* \
                   "
INSANE_SKIP_${PN}-dev += "staticdev"
FILES_${PN}-dbg += "${base_libdir}/.debug/libgcc_s.so.*.debug"

FILES_MIRRORS =. "\
    ${libdir}/${EXTERNAL_TARGET_SYS}/${BINV}/|${external_libroot}/\n \
    ${libdir}/${EXTERNAL_TARGET_SYS}/|${libdir}/gcc/${EXTERNAL_TARGET_SYS}/\n \
"

# Follow any symlinks in the libroot (multilib build) to the main
# libroot and include any symlinks there that link to our libroot.
python () {
    import pathlib

    def get_links(p):
        return (c for c in p.iterdir() if c.is_symlink())

    libroot = d.getVar('EXTERNAL_TOOLCHAIN_LIBROOT')
    if libroot != 'UNKNOWN':
        sysroot = d.getVar('EXTERNAL_TOOLCHAIN_SYSROOT')
        libroot = pathlib.Path(libroot)
        for child in get_links(libroot):
            link_dest = child.resolve(strict=True)
            for other_child in get_links(link_dest):
                if other_child.resolve() == libroot.resolve():
                    relpath = other_child.relative_to(sysroot)
                    d.appendVar('SYSROOT_DIRS', ' /' + str(relpath.parent))
                    d.appendVar(d.expand('FILES_${PN}-dev'), ' /' + str(relpath))
}

do_install_extra () {
    if [ -e "${D}${libdir}/${EXTERNAL_TARGET_SYS}" ] && [ -z "${MLPREFIX}" ]; then
        if ! [ -e "${D}${libdir}/${TARGET_SYS}" ]; then
            ln -s "${EXTERNAL_TARGET_SYS}" "${D}${libdir}/${TARGET_SYS}"
        fi
    fi
}

do_package[prefuncs] += "add_sys_symlink"

python add_sys_symlink () {
    import pathlib
    target_sys = pathlib.Path(d.expand('${D}${libdir}/${TARGET_SYS}'))
    if target_sys.exists():
        pn = d.getVar('PN')
        d.appendVar('FILES_%s-dev' % pn, ' ${libdir}/${TARGET_SYS}')
}
