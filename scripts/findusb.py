"""Find USB devices."""

import sys
from typing import Iterable
from serial.tools.list_ports import comports


def find_devices(*, vendor_id: int, product_id: int) -> Iterable[str]:
    for port in comports():
        if port.vid == vendor_id and port.pid == product_id:
            yield port.device


def main() -> None:
    if len(sys.argv) != 3:
        print('Usage: findusb.py vendor_id port_id', file=sys.stderr)
        sys.exit(1)
    for device in find_devices(vendor_id=int(sys.argv[1]),
                               product_id=int(sys.argv[2])):
        print(device)


if __name__ == '__main__':
    main()
