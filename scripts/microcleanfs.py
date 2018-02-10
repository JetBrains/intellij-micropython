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


"""Remove the contents of the file system on a MicroPython device.

Usage:
    microcleanfs PORT [options]

    Options:
    -f --force     Remove ALL files from filesystem w/o preserving excludes
"""

import traceback
from typing import List

import sys
import time

from ampy.files import Files
from ampy.pyboard import Pyboard, PyboardError
from docopt import docopt


def main(args: List[str]) -> None:
    opts = docopt(__doc__, argv=args)
    port = opts['PORT']
    force = opts['--force']

    print('Connecting to {}'.format(port), file=sys.stderr)
    board = Pyboard(port)
    files = Files(board)

    # Specifying subdirectories DOES NOT work as they will be deleted when the
    # parent directory is deleted. Specifying top level directories DOES work.
    exclude_files = ['boot.py']

    print('Removing the contents of the file system')
    wait_for_board()
    for name in files.ls():
        if force or name not in exclude_files:
            try:
                files.rm(name)
            except (RuntimeError, PyboardError):
                try:
                    files.rmdir(name)
                except (RuntimeError, PyboardError):
                    print('Unknown Error removing file {}'.format(name),
                          file=sys.stderr)

    print('Done')


def wait_for_board() -> None:
    """Wait for some ESP8266 devices to become ready for REPL commands."""
    time.sleep(0.5)


if __name__ == '__main__':
    try:
        main(sys.argv[1:])
        exitcode = 0
    except:
        traceback.print_exc()
        exitcode = 1
    finally:
        input('Press ENTER to continue')
    sys.exit(exitcode)
