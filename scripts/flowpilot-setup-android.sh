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
pkg update && yes | pkg upgrade
pkg install -y proot-distro termux-api net-tools openssh wget

# download latest apks
LATEST_FLOWPILOT_APK=$(curl -s https://api.github.com/repos/flowdriveai/flowpilot/releases/latest | grep browser_download_url | cut -d '"' -f 4)
wget -q --show-progress --no-clobber --tries=5 $LATEST_FLOWPILOT_APK -P /sdcard/flowpilot/Downloads

TERMUX_API_APK='https://f-droid.org/repo/com.termux.api_51.apk'
wget -q --show-progress --no-clobber --tries=5 $TERMUX_API_APK -P /sdcard/flowpilot/Downloads

# setup startup commands
add_to_file 'sshd' ~/.bashrc
add_to_file 'termux-wake-lock' ~/.bashrc

function setup_nonroot_env {
        proot-distro install ubuntu || info "Ubuntu already installed.. skipping"
        add_to_file $'alias login=\'proot-distro login ubuntu\'' ~/.bashrc

        proot_exec "echo 'export ANDROID_DATA=\'\'' >> ~/.bashrc"
        proot_exec "apt update && apt install -y sudo git"
        proot_exec "DEBIAN_FRONTEND=noninteractive apt-get install -y tzdata"
        proot_exec "sudo apt install -y software-properties-common"
        proot_exec "yes | sudo add-apt-repository ppa:deadsnakes/ppa" || true
        proot_exec "sudo apt install -y python3.9 python3.9-dev python3.9-distutils python-is-python3 python3-pip"
        proot_exec "sudo update-alternatives --install /usr/bin/python python /usr/bin/python3.9 1"
        proot_exec "sudo update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.9 1"

        proot_exec "[ ! -d 'flowpilot' ] && git clone https://github.com/flowdriveai/flowpilot.git" || true
        proot_exec "cd flowpilot && git submodule update --init"

        proot_exec "cd flowpilot && ANDROID_DATA='' ./get_dependencies.sh"
        proot_exec "cd flowpilot && ANDROID_DATA='' scons"
}

# TODO: fix this
function setup_root_env {
        ROOTFS_TAR="ubuntu-focal-core-cloudimg-arm64-root-2020.12.10.tar.gz"
        wget -q --show-progress --no-clobber --tries=5 https://github.com/termux/proot-distro/releases/download/v1.2-ubuntu-focal-rootfs/$ROOTFS_TAR -P /sdcard/flowpilot/Downloads/
        mkdir -p chroot
        sudo tar xfp /sdcard/flowpilot/Download/$ROOTFS_TAR -C ./chroot

        echo """
            #!/data/data/com.termux/files/usr/bin/sh

            # prepare mount external storage
            mkdir -p ./chroot/sdcard

            # fix /data mount options
            mount -o remount,dev,suid /data

            mount --bind /dev ./chroot/dev
            mount --bind /sys ./chroot/sys
            mount --bind /proc ./chroot/proc
            mount --bind /dev/pts ./chroot/dev/pts
            mount --bind /sdcard ./chroot/sdcard

            # disable termux-exec
            unset LD_PRELOAD

            export PATH=/bin:/sbin:/usr/bin:/usr/sbin
            export TERM=$TERM
            export TMPDIR=/tmp

            chroot ./chroot /bin/su - root
        """ >> login-root.sh

        chmod +x run-root.sh
}


setup_nonroot_env
