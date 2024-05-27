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


"""Upload files and directories onto a MicroPython device.

Usage:
    microupload PORT PATH [-X PATH]... [options]

Options:
    -X --exclude=PATH       Path to exclude, may be repeated.
    -C --chdir=PATH         Change current directory to path.
    -v --verbose            Verbose output.
"""

import time
import sys
import os
from contextlib import suppress
from typing import List, Iterable, TypeVar, Sequence, Set

from docopt import docopt
from mpremote.transport_serial import SerialTransport
from mpremote.transport import TransportError

__all__ = []

verbose = False
T = TypeVar('T')


def main(args: List[str]) -> None:
    global verbose
    opts = docopt(__doc__, argv=args)
    verbose = opts['--verbose']
    root = opts['PATH']

    chdir = opts['--chdir']
    if chdir:
        os.chdir(chdir)

    port = opts['PORT']
    print('Connecting to {}'.format(port), file=sys.stderr)
    rel_root = os.path.relpath(root, os.getcwd())

    transport = SerialTransport(device=port)

    if os.path.isdir(root):
        to_upload = [os.path.join(rel_root, x)
                     for x in list_files(root, opts['--exclude'])]
    else:
        to_upload = [rel_root]

    created_cache = set()
    for path in progress('Uploading files', to_upload):
        local_path = os.path.abspath(path)
        remote_path = os.path.normpath(path).replace(os.path.sep, '/')
        if verbose:
            print('\n{} -> {}'.format(local_path, remote_path),
                  file=sys.stderr, flush=True)
        remote_dir = os.path.dirname(path)
        if remote_dir:
            make_dirs(transport, remote_dir, created_cache)
        transport.enter_raw_repl(soft_reset=False)
        transport.fs_put(local_path, remote_path)

    print('Soft reboot', file=sys.stderr, flush=True)
    soft_reset(transport)


def make_dirs(transport: SerialTransport, path: str,
              created_cache: Set[str] = None) -> None:
    """Make all the directories the specified relative path consists of."""
    if path == '.':
        return
    if created_cache is None:
        created_cache = set()
    parent = os.path.dirname(path)
    if parent and parent not in created_cache:
        make_dirs(transport, parent, created_cache)
    posix_path = path.replace(os.path.sep, '/')
    try:
        posix_path = path.replace(os.path.sep, '/')
        transport.fs_mkdir(posix_path)
        created_cache.add(path)
    except TransportError:
        pass


def soft_reset(transport: SerialTransport) -> None:
    """Perform soft-reset of the ESP8266 board."""
    transport.enter_raw_repl(soft_reset=True)


def list_files(path: str, excluded: List[str]) -> Iterable[str]:
    """List relative file paths inside the given path."""
    excluded = {os.path.abspath(x) for x in excluded}
    for root, dirs, files in os.walk(path):
        abs_root = os.path.abspath(root)
        for d in list(dirs):
            if os.path.join(abs_root, d) in excluded or d.startswith('.'):
                dirs.remove(d)
        for f in files:
            if os.path.join(abs_root, f) not in excluded and not f.startswith('.'):
                yield os.path.relpath(os.path.join(root, f), path)


def progress(msg: str, xs: Sequence[T]) -> Iterable[T]:
    """Show progress while iterating over a sequence."""
    size = len(xs)
    sys.stderr.write('\r{}: 0% (0/{})'.format(msg, size))
    sys.stderr.flush()
    for i, x in enumerate(xs, 1):
        yield x
        s = '{0}: {1}% ({2}/{3})'.format(msg, int(i * 100 / size), i, size)
        sys.stderr.write('\r' + s)
        sys.stderr.flush()
    sys.stderr.write('\n')
    sys.stderr.flush()


if __name__ == '__main__':
    main(sys.argv[1:])
