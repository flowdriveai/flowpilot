# Android USB Host Missed Cycles

This note explains the `missed cycles` warning seen in `boardd` on Android USB
host setups, the most likely causes, and the checks that help separate a bad
USB path from normal scheduler jitter.

## What the warning means

`selfdrive/boardd/boardd.cc` runs `boardd_can_recv` at 100 Hz. Each loop has a
10 ms budget to read CAN data from panda over USB, publish the `can` message,
and sleep until the next frame deadline.

When the thread reaches the end of a loop after the deadline, `boardd` logs:

```text
missed cycles (<count>) <remaining> recv=<receive_time>
```

`remaining` is negative nanoseconds relative to the expected 100 Hz deadline.
`recv` is the time spent inside the panda CAN USB receive calls for that loop.
If `recv` is close to or above 10 ms, the USB read path is consuming the cycle
budget by itself.

## Android no-root path

Flowpilot has two panda connection paths:

- Normal desktop/rooted path: `pandad.py` finds pandas by serial, then runs
  `./boardd <serial>`.
- Android no-root path: `pandad.py` calls `termux-usb -l`, probes devices with
  `termux-usb -r -e ./ispanda <fd>`, then launches
  `termux-usb -r -e ./boardd <fd>`.

The no-root path passes an Android USB permission file descriptor into
`boardd`. `Panda::Panda(const int fd, ...)` wraps that descriptor with
`libusb_wrap_sys_device`, and `Panda::can_receive()` reads endpoint `0x81` with
`libusb_bulk_transfer`.

That means Android no-root operation depends on:

- the phone ROM's USB host implementation,
- Termux/Android permission and descriptor handoff,
- libusb behavior on the wrapped descriptor,
- Android scheduler latency while `boardd_can_recv` tries to hold a 100 Hz
  loop.

## Why missed cycles happen

The direct code-level risk was an unbounded CAN receive bulk transfer. The repo
default `TIMEOUT` is `0`, and `Panda::can_receive()` previously called
`usb_bulk_read()` without overriding it. In libusb, a zero timeout means the
bulk transfer may wait indefinitely. On Android ROMs where USB host access is
slow, suspended, power-throttled, or delayed by the permission FD path, one read
can consume more than the 10 ms loop budget.

Other contributors can make the same symptom worse:

- poor OTG cable, loose connector, or underpowered panda/phone USB path,
- Android battery optimization, doze, thermal throttling, or screen-off CPU
  throttling,
- ROM-specific USB host bugs or aggressive power policy,
- multiple pandas read sequentially in one 100 Hz loop,
- other boardd threads holding the same per-panda USB lock for control
  transfers,
- missing root-level real-time scheduling, CPU affinity, and IRQ affinity on
  Android no-root devices.

The old tici-specific hardware path explicitly kept a boardd core online and
assigned xHCI USB IRQs to that core. Android no-root devices usually cannot
recreate that setup.

## Code fix in this PR

This PR bounds the CAN receive bulk read:

```cpp
usb_bulk_read(0x81, data, RECV_SIZE, 5);
```

A timeout is not treated as a communication failure in the existing
`usb_bulk_read()` implementation; it returns the bytes received so far, or zero
bytes if no CAN data arrived during the timeout. `Panda::can_receive()` already
handles `recv <= 0` as a healthy empty receive, so this keeps behavior
compatible while preventing one slow Android USB read from blocking the 100 Hz
loop without a limit.

The receive timeout is set to 5 ms to match the existing CAN transmit bulk
write timeout. That leaves part of the 10 ms boardd cycle for publishing and
sleep recovery on the common single-panda Android path.

The PR also makes the loop deadline arithmetic signed and logs `recv=<ns>` with
missed-cycle warnings. That makes it clear whether the loop was late because
USB receive consumed the budget or because later scheduler/publish work delayed
the thread.

## Field checklist

Use this order when diagnosing a phone that still logs missed cycles:

1. Confirm that the log includes `recv=<ns>`.
2. If `recv` is near or above `10000000`, focus on USB host latency.
3. If `recv` is small but missed cycles continue, focus on Android scheduling,
   CPU governor, thermal throttling, or other high-priority processes.
4. Keep the phone awake, disable battery optimization for Termux/Flowpilot, and
   avoid screen-off power saving while testing.
5. Test a short known-good OTG cable, then a powered hub if the phone cannot
   keep panda powered reliably.
6. Disconnect other USB devices from the phone while testing.
7. Compare the same panda and cable on Linux/desktop. If the warning disappears,
   the phone/ROM USB host path is the likely source.
8. On rooted setups, test performance governor, real-time priority, and CPU/IRQ
   affinity for boardd. Do not apply these blindly on no-root Termux; this repo
   already disables some real-time helpers because they can freeze Android
   chroot setups.

To collect a first diagnostic bundle, run this either inside the Android
Termux/flowpilot environment or from a desktop with `adb` connected:

```sh
./scripts/android_usb_host_missed_cycles_triage.sh
```

The script auto-detects local Android/Termux mode versus `adb` mode. It records
USB host features, `dumpsys usb`, Type-C or USB role sysfs files, `termux-usb -l`
when available, filtered USB/boardd logcat lines, thermal state, and a summary
template. Review the generated directory before posting it publicly because
device logs can contain private local details.

## When a guide is enough

If the patched build no longer logs large `recv` values but a specific ROM
still drops USB, this is likely outside `boardd` itself. The practical fix is a
device/ROM/cable/power change, or running Flowpilot through a rooted/native
environment where USB scheduling and power policy can be controlled.

## References

- Android USB host mode lets an Android device power the bus and enumerate USB
  devices: <https://developer.android.com/develop/connectivity/usb/host>
- libusb synchronous bulk transfer uses milliseconds for `timeout`, and `0`
  means unlimited: <https://libusb.sourceforge.io/api-1.0/group__libusb__syncio.html>
