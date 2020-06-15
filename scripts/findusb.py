# Copyright 2000-2017 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Find USB devices.

Usage:
    findusb [VID:PID...]

Arguments:
    VID:PID       Vendor ID and Product ID for your USB device.

If you run it with no arguments, it outputs the list of all the USB devices it
has managed to find.
"""

import sys
from typing import Iterable, Tuple, List

from docopt import docopt
from serial.tools.list_ports import comports


def find_devices(ids: List[Tuple[int, int]]) -> Iterable[str]:
    for port in comports():
        if (port.vid, port.pid) in ids:
            yield port.device


def parse_int(s: str) -> int:
    if s.startswith('0x'):
        return int(s, 16)
    else:
        return int(s)


def parse_id(arg: str) -> Tuple[int, int]:
    vendor_id, product_id = arg.split(':', 1)
    return parse_int(vendor_id), parse_int(product_id)


def main() -> None:
    opts = docopt(__doc__, argv=sys.argv[1:])
    vid_pid_list = opts['VID:PID']
    if vid_pid_list:
        ids = [parse_id(arg) for arg in sys.argv[1:]]
        for device in find_devices(ids):
            print(device)
    else:
        for port in comports():
            if port.vid is not None and port.pid is not None:
                print('%s: 0x%02x:0x%02x' % (port.device, port.vid, port.pid))


if __name__ == '__main__':
    main()
