#!/usr/bin/env bash
set -euo pipefail

out_dir="${1:-android-usb-host-triage-$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$out_dir"

run() {
  local name="$1"
  shift
  {
    printf '$'
    printf ' %q' "$@"
    printf '\n\n'
    "$@" 2>&1 || true
  } > "$out_dir/$name.txt"
}

run host_features adb shell pm list features
run usb_state adb shell dumpsys usb
run build_props adb shell sh -c 'getprop ro.build.fingerprint; getprop ro.product.manufacturer; getprop ro.product.model; getprop ro.build.version.release'
run usb_roles adb shell sh -c 'for f in /sys/class/usb_role/*/role /sys/class/typec/*/data_role; do [ -e "$f" ] && echo "$f=$(cat "$f")"; done'
run usb_role_paths adb shell sh -c 'find /sys -path "*usb*" \( -name role -o -name mode \) 2>/dev/null | head -100'
run logcat_usb adb logcat -d
run dmesg_usb adb shell su -c 'dmesg | grep -Ei "usb|dwc|musb|typec|vbus|over-current|reset|disconnect|libusb|panda|boardd" | tail -200'
run usb_debug adb shell su -c 'cat /sys/kernel/debug/usb/devices 2>/dev/null | head -200'
run autosuspend adb shell su -c 'cat /sys/module/usbcore/parameters/autosuspend 2>/dev/null'
run thermal adb shell dumpsys thermalservice
run top_threads adb shell top -H -n 1

grep -Eai 'UsbHost|UsbDevice|UsbPort|libusb|boardd|panda|missed cycles|overflow|timeout|disconnect|reset' \
  "$out_dir/logcat_usb.txt" > "$out_dir/logcat_usb_filtered.txt" || true

cat > "$out_dir/README.txt" <<EOF
Attach this directory to a flowpilot USB host mode report.

Before sharing publicly, review the files for serial numbers, account names,
Wi-Fi SSIDs, or other device-specific information that you do not want to post.
EOF

printf 'Wrote %s\n' "$out_dir"
