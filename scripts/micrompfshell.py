
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

    sys.argv = [sys.argv[0], '-c', '--nocolor']
    main()

if __name__ == '__main__':
    try:
        main()
        input("Press ENTER to exit")
    except Exception:
        sys.stderr.write(traceback.format_exc())
        input("Press ENTER to continue")
        sys.exit(1)
