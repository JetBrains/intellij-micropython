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

"""
A simple shim around PySerial that detects the correct port to which the
MicroPython device is connected and attempts to make a serial connection to it
in order to bring up the Python REPL.

Based on https://github.com/ntoll/microrepl by Nicholas H.Tollervey and the
contributors.
"""

import errno
import sys
from time import sleep
import traceback

import serial
import serial.tools.miniterm
from serial.tools.miniterm import Miniterm, key_description

BAUDRATE = 115200
PARITY = 'N'


if sys.version_info >= (3, 0):
    def character(b):
        return b.decode('latin1')
else:
    def character(b):
        return b


def connect_miniterm(port):
    try:
        ser = serial.Serial(port, BAUDRATE, parity=PARITY, rtscts=False,
                            xonxoff=False)
        return Miniterm(ser, echo=False)
    except serial.SerialException as e:
        if e.errno == errno.ENOENT:
            sys.stderr.write(
                "Device %r not found. Check your "
                "MicroPython device path settings.\n" % port)
        elif e.errno == errno.EBUSY:
            # Device is busy. Explain what to do.
            sys.stderr.write(
                "Found the device, but it is busy. "
                "Wait up to 20 seconds, or "
                "press the reset button on the "
                "back of the device next to the yellow light; "
                "then try again.\n"
            )
        elif e.errno == errno.EACCES:
            sys.stderr.write("Found the device, but could not connect.\n".format(port))
            sys.stderr.write('%s\n' % (str(e),))
            sys.stderr.write('On linux, try adding yourself to the "dialout" group:\n')
            sys.stderr.write('sudo usermod -a -G dialout <your-username>\n')
        else:
            # Try to be as helpful as possible.
            sys.stderr.write("Found the device, but could not connect via" +
                             " port %r: %s\n" % (port, e))
            sys.stderr.write("I'm not sure what to suggest. :-(\n")
        input("Press ENTER to continue")
        sys.exit(1)


def main():
    """
    The function that actually runs the REPL.
    """
    if len(sys.argv) != 2:
        print("Usage: microrepl.py /path/to/device")

    port = sys.argv[1]
    print('Device path', port)
    serial.tools.miniterm.EXITCHARCTER = character(b'\x1d')
    miniterm = connect_miniterm(port)
    # Emit some helpful information about the program and MicroPython.
    shortcut_message = 'Quit: {} | Stop program: Ctrl+C | Reset: Ctrl+D\n'
    help_message = 'Type \'help()\' (without the quotes) then press ENTER.\n'
    exit_char = key_description(serial.tools.miniterm.EXITCHARCTER)
    sys.stderr.write(shortcut_message.format(exit_char))
    sys.stderr.write(help_message)
    # Start everything.
    miniterm.set_rx_encoding('utf-8')
    miniterm.set_tx_encoding('utf-8')
    miniterm.start()
    sleep(0.5)
    miniterm.serial.write(b'\x03')  # Connecting stops the running program.
    try:
        miniterm.join(True)
    except KeyboardInterrupt:
        pass
    sys.stderr.write('\nEXIT - see you soon... :-)\n')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        sys.stderr.write(traceback.format_exc())
        input("Press ENTER to continue")
        sys.exit(1)
