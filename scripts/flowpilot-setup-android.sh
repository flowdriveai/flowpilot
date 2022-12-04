#!/data/data/com.termux/files/usr/bin/bash

set -e

# White
msg() {
    echo -e "$@"
}

# Yellow
info() {
    printf "\033[1;33m$@\033[0m\n"
}

# Green
success() {
    printf "\033[0;32m$@\033[0m\n"
}

# Red
fail() {
    printf "\033[0;31m$@\033[0m\n"
    exit 1
}

proot_exec() {
    proot-distro login ubuntu -- eval "$1"
}

add_to_file() {
    grep -qF -- "$1" "$2" || echo "$1" >> "$2"
}

# get access to external storage
yes | termux-setup-storage

# install base packages
pkg install -y proot-distro termux-api net-tools openssh wget tsu

# download latest apks
FLOWPILOT_APK_URL=$(curl -s https://api.github.com/repos/flowdriveai/flowpilot/releases/latest | grep browser_download_url | cut -d '"' -f 4)
wget -q --show-progress --no-clobber --tries=5 $FLOWPILOT_APK_URL -P /sdcard/flowpilot/Downloads

TERMUX_API_APK_URL='https://f-droid.org/repo/com.termux.api_51.apk'
wget -q --show-progress --no-clobber --tries=5 $TERMUX_API_APK_URL -P /sdcard/flowpilot/Downloads

INSTALL_SCRIPT_URL=''
wget -q --show-progress --no-clobber --tries=5 $INSTALL_SCRIPT_URL -P /sdcard/flowpilot/Download

# setup startup commands
add_to_file 'sshd' ~/.bashrc
add_to_file 'termux-wake-lock' ~/.bashrc

setup_nonroot_env() {
        proot-distro install ubuntu || info "ubuntu already installed.. skipping"
        add_to_file $'alias login=\'proot-distro login ubuntu\'' ~/.bashrc

        ln -rs $PREFIX/var/lib/proot-distro/installed-rootfs/ubuntu $HOME/flowpilot_env
        cp /sdcard/flowpilot/Download/install-flowpilot $HOME/flowpilot_env/bin
}

# TODO: fix this
setup_root_env() {
        

        ROOTFS_TAR="ubuntu-focal-core-cloudimg-arm64-root-2020.12.10.tar.gz"
        wget -q --show-progress --no-clobber --tries=5 https://github.com/termux/proot-distro/releases/download/v1.2-ubuntu-focal-rootfs/$ROOTFS_TAR -P /sdcard/flowpilot/Downloads/
        
        CHROOT=/data/data/com.termux/files/home/flowpilot_env_root
        mkdir -p $CHROOT
        sudo tar --skip-old-files xfp /sdcard/flowpilot/Downloads/$ROOTFS_TAR -C $CHROOT

        cp /sdcard/flowpilot/Download/install-flowpilot $HOME/flowpilot_env_root/bin

        rm /data/data/com.termux/files/usr/bin/login-root
        echo "
            #!/data/data/com.termux/files/usr/bin/bash

            # prepare mount external storage
            mkdir -p $CHROOT/sdcard

            # fix /data mount options
            mount -o remount,dev,suid /data

            mount --bind /dev $CHROOT/dev
            mount --bind /sys $CHROOT/sys
            mount --bind /proc $CHROOT/proc
            mount --bind /dev/pts $CHROOT/dev/pts
            mount --bind /sdcard $CHROOT/sdcard

            # disable termux-exec
            unset LD_PRELOAD

            export PATH=/bin:/sbin:/usr/bin:/usr/sbin
            export TERM=\$TERM
            export TMPDIR=/tmp

            chroot $CHROOT /bin/su - root
        " >> /data/data/com.termux/files/usr/bin/login-root
        chmod +x /data/data/com.termux/files/usr/bin/login-root
}

setup_nonroot_env
setup_root_env

success "Flowpilot environments installed."
success "login to non-root env with `login`"
success "login to root env with `sudo login-root`"
