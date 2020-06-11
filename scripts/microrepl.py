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

import ctypes
import errno
import os
import platform
import sys
import traceback
from time import sleep

from serial import Serial, SerialException
from serial.tools import miniterm
from serial.tools.miniterm import Miniterm, key_description

if os.name == 'nt':
    import msvcrt

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
        ser = Serial(port, BAUDRATE, parity=PARITY, rtscts=False, xonxoff=False)
        return Miniterm(ser, echo=False)
    except SerialException as e:
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


class Windows10Console(miniterm.Console):
    """Patched Console to support ANSI control keys on Windows 10.

    Based on a fix by Cefn Hoile https://github.com/pyserial/pyserial/pull/351
    that hasn't been merged into pyserial yet.
    """

    fncodes = {
        ';': '\1bOP',  # F1
        '<': '\1bOQ',  # F2
        '=': '\1bOR',  # F3
        '>': '\1bOS',  # F4
        '?': '\1b[15~',  # F5
        '@': '\1b[17~',  # F6
        'A': '\1b[18~',  # F7
        'B': '\1b[19~',  # F8
        'C': '\1b[20~',  # F9
        'D': '\1b[21~',  # F10
    }
    navcodes = {
        'H': '\x1b[A',  # UP
        'P': '\x1b[B',  # DOWN
        'K': '\x1b[D',  # LEFT
        'M': '\x1b[C',  # RIGHT
        'G': '\x1b[H',  # HOME
        'O': '\x1b[F',  # END
        'R': '\x1b[2~',  # INSERT
        'S': '\x1b[3~',  # DELETE
        'I': '\x1b[5~',  # PGUP
        'Q': '\x1b[6~',  # PGDN
    }

    def __init__(self) -> None:
        super().__init__()
        # ANSI handling available through SetConsoleMode since Windows 10 v1511
        # https://en.wikipedia.org/wiki/ANSI_escape_code#cite_note-win10th2-1
        if platform.release() == '10' and int(platform.version().split('.')[2]) > 10586:
            ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004
            import ctypes.wintypes as wintypes
            if not hasattr(wintypes, 'LPDWORD'):  # PY2
                wintypes.LPDWORD = ctypes.POINTER(wintypes.DWORD)
            SetConsoleMode = ctypes.windll.kernel32.SetConsoleMode
            GetConsoleMode = ctypes.windll.kernel32.GetConsoleMode
            GetStdHandle = ctypes.windll.kernel32.GetStdHandle
            mode = wintypes.DWORD()
            GetConsoleMode(GetStdHandle(-11), ctypes.byref(mode))
            if (mode.value & ENABLE_VIRTUAL_TERMINAL_PROCESSING) == 0:
                SetConsoleMode(GetStdHandle(-11), mode.value | ENABLE_VIRTUAL_TERMINAL_PROCESSING)
                self._saved_cm = mode

    def __del__(self) -> None:
        super().__del__()
        try:
            ctypes.windll.kernel32.SetConsoleMode(ctypes.windll.kernel32.GetStdHandle(-11), self._saved_cm)
        except AttributeError:  # in case no _saved_cm
            pass

    def getkey(self) -> str:
        while True:
            z = msvcrt.getwch()
            if z == chr(13):
                return chr(10)
            elif z is chr(0) or z is chr(0xe0):
                try:
                    code = msvcrt.getwch()
                    if z is chr(0):
                        return self.fncodes[code]
                    else:
                        return self.navcodes[code]
                except KeyError:
                    pass
            else:
                return z


def main():
    """
    The function that actually runs the REPL.
    """
    if len(sys.argv) != 2:
        print("Usage: microrepl.py /path/to/device")
        sys.exit(1)

    port = sys.argv[1]
    print('Device path', port)

    term = connect_miniterm(port)

    if os.name == 'nt':
        term.console = Windows10Console()

    # Emit some helpful information about the program and MicroPython.
    shortcut_message = 'Quit: {} | Stop program: Ctrl+C | Reset: Ctrl+D\n'
    help_message = 'Type \'help()\' (without the quotes) then press ENTER.\n'
    exit_character = character(b'\x1d')
    term.exit_character = exit_character
    exit_char = key_description(exit_character)
    sys.stderr.write(shortcut_message.format(exit_char))
    sys.stderr.write(help_message)
    # Start everything.
    term.set_rx_encoding('utf-8')
    term.set_tx_encoding('utf-8')
    term.start()
    sleep(0.5)
    term.serial.write(b'\x03')  # Connecting stops the running program.
    try:
        term.join(True)
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
