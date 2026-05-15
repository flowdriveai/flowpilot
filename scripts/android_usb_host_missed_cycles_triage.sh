#!/usr/bin/env bash
set -euo pipefail

out_dir="${1:-android-usb-host-missed-cycles-$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$out_dir"

quote_cmd() {
  printf '$'
  printf ' %q' "$@"
  printf '\n\n'
}

run_cmd() {
  local name="$1"
  shift
  {
    quote_cmd "$@"
    "$@" 2>&1 || true
  } > "$out_dir/$name.txt"
}

run_local_shell() {
  local name="$1"
  shift
  run_cmd "$name" sh -c "$*"
}

run_adb_shell() {
  local name="$1"
  shift
  run_cmd "$name" adb shell "$*"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

mode="local"
if ! have_cmd getprop && have_cmd adb && adb get-state >/dev/null 2>&1; then
  mode="adb"
fi

{
  echo "Flowpilot Android USB host missed-cycles triage"
  echo "mode: $mode"
  echo "created_at: $(date -Iseconds 2>/dev/null || date)"
  echo
  echo "Review these files before sharing them publicly. Device logs can contain"
  echo "serial numbers, nearby network names, account names, and vehicle details."
} > "$out_dir/README.txt"

if [ "$mode" = "local" ]; then
  run_local_shell build_props 'getprop ro.build.fingerprint; getprop ro.product.manufacturer; getprop ro.product.model; getprop ro.build.version.release; uname -a'
  run_local_shell host_features 'pm list features | grep -i "android.hardware.usb.host" || true'
  run_local_shell usb_state 'dumpsys usb'
  run_local_shell usb_roles 'for f in /sys/class/usb_role/*/role /sys/class/typec/*/data_role; do [ -e "$f" ] && echo "$f=$(cat "$f")"; done'
  run_local_shell usb_role_paths 'find /sys -path "*usb*" \( -name role -o -name mode \) 2>/dev/null | head -100'
  run_local_shell processes 'ps -A 2>/dev/null | grep -Ei "boardd|pandad|termux|flowpilot" || true'
  run_local_shell thermal 'dumpsys thermalservice 2>/dev/null || true'
  run_local_shell top_threads 'top -H -n 1 2>/dev/null || top -n 1 2>/dev/null || true'
  if have_cmd termux-usb; then
    run_cmd termux_usb_list termux-usb -l
  else
    echo "termux-usb not found in PATH" > "$out_dir/termux_usb_list.txt"
  fi
  if have_cmd logcat; then
    run_cmd logcat_full logcat -d
  else
    echo "logcat not found in PATH" > "$out_dir/logcat_full.txt"
  fi
else
  run_adb_shell build_props 'getprop ro.build.fingerprint; getprop ro.product.manufacturer; getprop ro.product.model; getprop ro.build.version.release; uname -a'
  run_adb_shell host_features 'pm list features | grep -i "android.hardware.usb.host" || true'
  run_adb_shell usb_state 'dumpsys usb'
  run_adb_shell usb_roles 'for f in /sys/class/usb_role/*/role /sys/class/typec/*/data_role; do [ -e "$f" ] && echo "$f=$(cat "$f")"; done'
  run_adb_shell usb_role_paths 'find /sys -path "*usb*" \( -name role -o -name mode \) 2>/dev/null | head -100'
  run_adb_shell processes 'ps -A 2>/dev/null | grep -Ei "boardd|pandad|termux|flowpilot" || true'
  run_adb_shell thermal 'dumpsys thermalservice 2>/dev/null || true'
  run_adb_shell top_threads 'top -H -n 1 2>/dev/null || top -n 1 2>/dev/null || true'
  run_cmd logcat_full adb logcat -d
  run_cmd termux_usb_list adb shell 'command -v termux-usb >/dev/null 2>&1 && termux-usb -l || echo "termux-usb not found in adb shell PATH"'
fi

grep -Eai 'UsbHost|UsbDevice|UsbPort|libusb|boardd|pandad|panda|termux-usb|missed cycles|overflow|timeout|disconnect|reset|vbus|typec' \
  "$out_dir/logcat_full.txt" > "$out_dir/logcat_usb_filtered.txt" || true

cat > "$out_dir/summary_template.md" <<'EOF'
# Flowpilot Android USB Host Triage Summary

- Device:
- ROM/build fingerprint:
- Android version:
- Panda type:
- Cable, OTG adapter, hub, and power path:
- Local Termux mode or adb mode:
- `android.hardware.usb.host` present:
- `dumpsys usb` host/source role while panda attached:
- `termux-usb -l` sees panda:
- First `missed cycles` log line:
- Nearby USB timeout/overflow/disconnect/reset lines:
- Thermal or CPU throttling evidence:
- Does the same panda/cable work on Linux or desktop:

Attach the full output directory only after reviewing it for private data.
EOF

printf 'Wrote %s\n' "$out_dir"
