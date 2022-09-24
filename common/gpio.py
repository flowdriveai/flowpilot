# hard-forked from https://github.com/commaai/openpilot/tree/05b37552f3a38f914af41f44ccc7c633ad152a15/selfdrive/common/gpio.py
def gpio_init(pin: int, output: bool) -> None:
  try:
    with open(f"/sys/class/gpio/gpio{pin}/direction", 'wb') as f:
      f.write(b"out" if output else b"in")
  except Exception as e:
    print(f"Failed to set gpio {pin} direction: {e}")


def gpio_set(pin: int, high: bool) -> None:
  try:
    with open(f"/sys/class/gpio/gpio{pin}/value", 'wb') as f:
      f.write(b"1" if high else b"0")
  except Exception as e:
    print(f"Failed to set gpio {pin} value: {e}")
