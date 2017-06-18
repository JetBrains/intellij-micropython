#!/usr/bin/env python
"""
A simple shim around PySerial that detects the correct port to which the
micro:bit is connected and attempts to make a serial connection to it in order
to bring up the Python REPL.
"""
from __future__ import print_function
import sys
import serial
import serial.tools.miniterm
from serial.tools.list_ports import comports
from serial.tools.miniterm import Console, Miniterm, key_description

console = Console()


MICROBIT_PID = 516
MICROBIT_VID = 3368
BAUDRATE = 115200
PARITY = 'N'
ARM = 'https://developer.mbed.org/handbook/Windows-serial-configuration'


if sys.version_info >= (3, 0):
    def character(b):
        return b.decode('latin1')
else:
    def character(b):
        return b


def find_microbit():
    """
    Returns the port for the first micro:bit found connected to the computer
    running this script. If no micro:bit is found, returns None.
    """
    ports = comports()
    platform = sys.platform
    if platform.startswith('linux'):
        for port in ports:
            if 'VID:PID=0D28:0204' in port[2].upper():
                return port[0]
    elif platform.startswith('darwin'):
        for port in ports:
            if 'VID:PID=0D28:0204' in port[2]:
                return port[0]
    elif platform.startswith('win'):
        for port in ports:
            if 'VID:PID=0D28:0204' in port[2]:
                return port[0]
        # No COM port found, so give an informative prompt.
        sys.stderr.write('Have you installed the micro:bit driver?\n')
        sys.stderr.write('For more details see: {}\n'.format(ARM))
    return None


def connect_miniterm(port):
    try:
        ser = serial.Serial(port, BAUDRATE, parity=PARITY, rtscts=False, xonxoff=False)
        return Miniterm(
            ser,
            echo=False,
            #convert_outgoing=2,
            #repr_mode=0,
        )
    except serial.SerialException as e:
        if e.errno == 16:
            # Device is busy. Explain what to do.
            sys.stderr.write(
                "Found micro:bit, but the device is busy. "
                "Wait up to 20 seconds, or "
                "press the reset button on the "
                "back of the device next to the yellow light; "
                "then try again.\n"
            )
        elif e.errno == 13:
            print("Found micro:bit, but could not connect.".format(port), file=sys.stderr)
            print(e, file=sys.stderr)
            print('On linux, try adding yourself to the "dialout" group', file=sys.stderr)
            print('sudo usermod -a -G dialout <your-username>', file=sys.stderr)

        else:
            # Try to be as helpful as possible.
            sys.stderr.write("Found micro:bit, but could not connect via" +
                             " port %r: %s\n" % (port, e))
            sys.stderr.write("I'm not sure what to suggest. :-(\n")
        sys.exit(1)


def main():
    """
    The function that actually runs the REPL.
    """
    port = find_microbit()
    print('port', port)
    if not port:
        sys.stderr.write('Could not find micro:bit. Is it plugged in?\n')
        sys.exit(0)
    serial.tools.miniterm.EXITCHARCTER = character(b'\x1d')
    miniterm = connect_miniterm(port)
    # Emit some helpful information about the program and MicroPython.
    shortcut_message = 'Quit: {} | Stop program: Ctrl+C | Reset: Ctrl+D\n'
    help_message = 'Type \'help()\' (without the quotes) then press ENTER.\n'
    exit_char = key_description(serial.tools.miniterm.EXITCHARCTER)
    sys.stderr.write(shortcut_message.format(exit_char))
    sys.stderr.write(help_message)
    # Start everything.
    console.setup()
    miniterm.set_rx_encoding('utf-8')
    miniterm.set_tx_encoding('utf-8')
    miniterm.start()
    miniterm.serial.write(b'\x03')  # Connecting stops the running program.
    try:
        miniterm.join(True)
    except KeyboardInterrupt:
        pass
    sys.stderr.write('\nEXIT - see you soon... :-)\n')


if __name__ == '__main__':
    main()
