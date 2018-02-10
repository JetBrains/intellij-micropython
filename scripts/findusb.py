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

"""Find USB devices."""

import sys
from typing import Iterable, Tuple, List
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
    vendor_id, port_id = arg.split(':', 1)
    return parse_int(vendor_id), parse_int(port_id)


def main() -> None:
    if len(sys.argv) < 2:
        print('Usage: findusb.py vendor_id:port_id...', file=sys.stderr)
        sys.exit(1)
    ids = [parse_id(arg) for arg in sys.argv[1:]]
    for device in find_devices(ids):
        print(device)


if __name__ == '__main__':
    main()
