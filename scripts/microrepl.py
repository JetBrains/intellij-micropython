"""
A simple shim around PySerial that detects the correct port to which the
MicroPython device is connected and attempts to make a serial connection to it
in order to bring up the Python REPL.
"""

import sys
import serial
import serial.tools.miniterm
from serial.tools.miniterm import Console, Miniterm, key_description
from time import sleep

console = Console()


BAUDRATE = 115200
PARITY = 'N'
ARM = 'https://developer.mbed.org/handbook/Windows-serial-configuration'


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
        if e.errno == 16:
            # Device is busy. Explain what to do.
            sys.stderr.write(
                "Found the device, but it is busy. "
                "Wait up to 20 seconds, or "
                "press the reset button on the "
                "back of the device next to the yellow light; "
                "then try again.\n"
            )
        elif e.errno == 13:
            print("Found the device, but could not connect.".format(port),
                  file=sys.stderr)
            print(e, file=sys.stderr)
            print('On linux, try adding yourself to the "dialout" group',
                  file=sys.stderr)
            print('sudo usermod -a -G dialout <your-username>', file=sys.stderr)

        else:
            # Try to be as helpful as possible.
            sys.stderr.write("Found the device, but could not connect via" +
                             " port %r: %s\n" % (port, e))
            sys.stderr.write("I'm not sure what to suggest. :-(\n")
        sys.exit(1)


def main():
    """
    The function that actually runs the REPL.
    """
    if len(sys.argv) != 2:
        print("Usage: microrepl.py /path/to/device")

    port = sys.argv[1]
    print('port', port)
    if not port:
        sys.stderr.write('Could not find the device. Is it plugged in?\n')
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
    sleep(0.5)
    miniterm.serial.write(b'\x03')  # Connecting stops the running program.
    try:
        miniterm.join(True)
    except KeyboardInterrupt:
        pass
    sys.stderr.write('\nEXIT - see you soon... :-)\n')


if __name__ == '__main__':
    main()
