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
    findusb

Outputs the list of all USB devices it has managed to find.
"""

from serial.tools.list_ports import comports

def main() -> None:
    for port in comports():
        if port.vid is not None and port.pid is not None:
            print('0x%02x:0x%02x %s' % (port.vid, port.pid, port.device))


if __name__ == '__main__':
    main()
