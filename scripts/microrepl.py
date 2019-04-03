# Copyright 2015 The Python Software Foundation (http://python.org/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import errno
import sys
from time import sleep
import traceback

if sys.version_info >= (3, 0):
    def character(b):
        return b.decode('latin1')
else:
    def character(b):
        return b

def main():

    import sys

    print(sys.argv)

    if len(sys.argv) == 1:
        sys.argv.append("")

    if len(sys.argv[1]) == 0:

        import serial.tools.list_ports
        print("looking for computer port...")
        plist = list(serial.tools.list_ports.comports())
        if len(plist) <= 0:
            print("serial not found!")
        else:
            sys.argv[1] = plist[len(plist) - 1][0].split('/')[-1]
    
    from mp.mpfshell import main

    sys.argv = [sys.argv[0], '-c', 'open', sys.argv[1] + ';', 'repl;', '-n', '--nocolor', '--nohelp']
    main()

if __name__ == '__main__':
    try:
        main()
        input("Press ENTER to exit")
    except Exception:
        sys.stderr.write(traceback.format_exc())
        input("Press ENTER to continue")
        sys.exit(1)
