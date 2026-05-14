# Android USB host mode and boardd missed cycles

This guide documents a repeatable way to diagnose slow or intermittent Panda USB
access on Android devices. It is intended for devices or ROMs where `boardd`
prints `missed cycles`, `Panda receive buffer full`, `Transmit buffer full`, or
libusb timeout/overflow messages while a Panda is connected.

## Why this happens

`selfdrive/boardd/boardd.cc` runs the CAN receive loop at 100 Hz. Each loop reads
CAN data from every connected Panda, publishes the `can` message, and sleeps
until the next 10 ms frame. If the loop is more than one frame late while
ignition is on, flowpilot logs:

```text
missed cycles
```

On Android builds, the usual bottleneck is not CAN parsing. It is USB host
stability or USB bulk transfer latency:

- The Android device leaves host mode, drops VBUS, or returns to gadget/device
  mode.
- The ROM exposes USB host APIs but does not keep the controller in host or OTG
  role under load.
- Panda power is marginal because the phone, OTG adapter, or hub cannot hold
  stable VBUS.
- The kernel uses a gadget-only or peripheral-first controller configuration.
- USB autosuspend, thermal throttling, or CPU scheduling stalls libusb long
  enough to miss the 10 ms boardd receive cadence.
- The app can see a USB file descriptor, but SELinux, permissions, or vendor USB
  services add retries or disconnects around `/dev/bus/usb`.

## Flowpilot code paths to inspect

- `selfdrive/boardd/boardd.cc`:
  - `can_recv_thread` has the 100 Hz loop and missed-cycle warning.
  - `usb_connect` switches Panda USB power mode to CDP on non-x86 builds.
- `selfdrive/boardd/panda.cc`:
  - `Panda::usb_bulk_read` uses libusb endpoint `0x81` for CAN receive.
  - `Panda::usb_bulk_write` sends CAN to endpoint `3` with a short timeout.
  - `Panda::can_receive` warns when a full receive buffer is returned.
- `android/AndroidManifest.xml` should declare USB host capability so Android
  tooling, ROM integrators, and package managers know this build expects host
  mode support.

## Fast triage checklist

Run these with the Panda connected through the exact cable, hub, and power setup
used in the vehicle.

To collect the main checks into files:

```sh
./scripts/android_usb_host_triage.sh
```

Review the output before sharing it publicly because device logs can contain
serial numbers, nearby network names, or other local details.

```sh
adb shell getprop ro.build.fingerprint
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell pm list features | grep -i 'android.hardware.usb.host'
adb shell dumpsys usb
adb shell 'ls -l /sys/class/usb_role /sys/class/typec 2>/dev/null || true'
adb shell 'find /sys -path "*usb*" -name role -o -name mode 2>/dev/null | head -50'
adb logcat -d | grep -Ei 'UsbHost|UsbDevice|UsbPort|libusb|boardd|panda|missed cycles|overflow|timeout'
```

Healthy hosts usually show all of the following:

- `android.hardware.usb.host` is present in `pm list features`.
- `dumpsys usb` shows a connected device and a host or source role while the
  Panda is attached.
- Kernel or Android USB logs do not repeatedly disconnect and reconnect the same
  device.
- `boardd` warnings do not correlate with `LIBUSB_ERROR_TIMEOUT`,
  `LIBUSB_ERROR_OVERFLOW`, or USB role changes.

If root access is available on a development build, these extra probes are more
direct:

```sh
adb shell su -c 'lsusb -t 2>/dev/null || true'
adb shell su -c 'cat /sys/kernel/debug/usb/devices 2>/dev/null | head -120'
adb shell su -c 'dmesg | grep -Ei "usb|dwc|musb|typec|vbus|over-current|reset|disconnect" | tail -120'
adb shell su -c 'cat /sys/module/usbcore/parameters/autosuspend 2>/dev/null || true'
```

## Throughput test

Before changing flowpilot, isolate USB throughput from the rest of the driving
stack.

1. Boot the device in the same ROM, kernel, governor, and thermal state used in
   the car.
2. Connect Panda through the intended cable and power path.
3. Keep the screen awake and stop unrelated heavy apps.
4. Run a USB bulk-transfer smoke test for at least 10 minutes.
5. Save timestamps for every timeout, overflow, disconnect, and reconnect.

Any libusb or PyUSB test is acceptable as long as it repeatedly opens the Panda
USB device and exercises bulk reads. For quick local checks on rooted or
developer devices, use a small Termux/Python script with `pyusb`, or build a
tiny libusb binary that:

- Finds vendor/product `0xbbaa:0xddcc`.
- Claims interface `0`.
- Repeatedly reads endpoint `0x81`.
- Records elapsed time, bytes read, timeout count, overflow count, and device
  disconnects.

Treat any repeated timeout cluster, overflow, or disconnect as a host-mode issue
until proven otherwise. A clean USB path should sustain the boardd cadence
without role changes or repeated transfer errors.

## ROM and kernel fixes

There is no single sysfs path that works for all Android devices. Check the
device tree and kernel driver first, then apply the matching fix for that USB
controller.

### Controller role

For Type-C or dual-role controllers, confirm the data role remains host while
flowpilot is running:

```sh
adb shell 'for f in /sys/class/usb_role/*/role /sys/class/typec/*/data_role; do [ -e "$f" ] && echo "$f=$(cat "$f")"; done'
```

If the role flips back to `device`, `none`, or `sink`, fix the ROM/device tree
instead of adding retries in boardd. Typical device-tree or kernel areas:

- `dr_mode = "host"` for dedicated car builds, or stable `dr_mode = "otg"` plus
  a working role switch for shared builds.
- Correct Type-C/extcon wiring so attach events select host mode.
- A VBUS regulator that is enabled in host mode and can supply the adapter or
  hub.
- Host-mode kernel options for the actual controller family, such as DWC3 or
  MUSB host/dual-role support, rather than gadget-only support.

### Gadget mode conflicts

Android often configures USB gadget functions through configfs for ADB, MTP,
RNDIS, or accessory mode. On ROMs that fight the host role, verify the gadget
stack is not re-binding while Panda is attached:

```sh
adb shell 'mount | grep configfs || true'
adb shell 'find /config/usb_gadget -maxdepth 3 -type l -o -type f 2>/dev/null | head -100'
```

For a dedicated flowpilot device, prefer a boot profile that keeps the external
port in host role while driving. If ADB is needed, use Wi-Fi ADB or a separate
debug port rather than letting the main Panda port alternate between gadget and
host roles.

### Power and cabling

Host mode can enumerate successfully and still fail under load if VBUS is weak.
Use this order when testing:

1. Short, known-good USB cable.
2. Powered OTG hub or Y-cable.
3. Phone on charger with the hub powering Panda, not the phone trying to power
   both Panda and itself.
4. No magnetic adapters or charge-only cables.
5. Repeat the 10-minute bulk-transfer test after each change.

If power changes remove timeouts or disconnects, document the working cable and
hub combination for that device/ROM.

### Autosuspend and scheduling

For rooted development builds, disable USB autosuspend during diagnosis:

```sh
adb shell su -c 'echo -1 > /sys/module/usbcore/parameters/autosuspend 2>/dev/null || true'
```

Also check CPU pressure:

```sh
adb shell top -H -n 1 | grep -E 'boardd|flowpilot|surface|thermal|kworker'
adb shell dumpsys thermalservice 2>/dev/null | head -120
```

If missed cycles align with thermal throttling, background work, or display
composition, prioritize governor and thermal tuning before changing Panda USB
timeouts.

## When to change boardd

Only change boardd after the Android host path is stable. Retries can hide the
symptom while making CAN latency worse.

Useful low-risk instrumentation:

- Log the duration of each Panda `can_receive` call.
- Count libusb timeout, overflow, and no-device errors separately.
- Include USB serial and hardware type in repeated USB warnings.
- Emit one summary per second instead of logging every single slow transfer.

Avoid these as first fixes:

- Increasing the CAN receive loop period beyond 10 ms.
- Silently dropping all timeout diagnostics.
- Retrying bulk reads indefinitely while ignition is on.
- Switching roles or power modes inside the hot CAN receive loop.

## Minimal issue report template

When reporting a device or ROM, include:

```text
Device:
ROM/build fingerprint:
Kernel version:
Android version:
Panda type:
Cable/hub/power path:
Does pm list features show android.hardware.usb.host:
dumpsys usb role while Panda attached:
10-minute bulk test result:
boardd logs around first missed cycles warning:
dmesg/logcat USB disconnect or reset lines:
Thermal or CPU throttling evidence:
```

## References

- Android USB host overview: https://developer.android.com/develop/connectivity/usb/host
- Android `<uses-feature>` manifest documentation: https://developer.android.com/guide/topics/manifest/uses-feature-element
- Linux USB host-side API documentation: https://docs.kernel.org/driver-api/usb/usb.html
- Linux USB gadget configfs documentation: https://docs.kernel.org/usb/gadget_configfs.html
