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
    -d --different          Only Upload if remote file is different to local file.
"""

import time
import sys
import os
from contextlib import suppress
from typing import List, Iterable, TypeVar, Sequence, Set

from docopt import docopt
from ampy.pyboard import Pyboard, PyboardError
from ampy.files import Files, DirectoryExistsError
from hashlib import sha1
import textwrap


__all__ = []

verbose = True
T = TypeVar('T')

def get_hash(self, filename):
    command = """
        import sys
        from uhashlib import sha1
        import ubinascii
        hash = sha1()
        with open('{0}', 'rb') as infile:
            while True:
                result = infile.read({1})
                hash.update(result)
                if result == b'':
                    break
            len = sys.stdout.write(ubinascii.hexlify(hash.digest()))
            infile.close()
    """.format(
        filename, 32
    )
    self._pyboard.enter_raw_repl()
    try:
        out = self._pyboard.exec_(textwrap.dedent(command))
    except PyboardError as ex:
        # Check if this is an OSError #2, i.e. file doesn't exist and
        # rethrow it as something more descriptive.
        if ex.args[2].decode("utf-8").find("OSError: [Errno 2] ENOENT") != -1:
            self._pyboard.exit_raw_repl()
            return ""
        else:
            raise ex
    self._pyboard.exit_raw_repl()
    return out.decode('utf-8')

def get_size(self, filename):
    print(filename)
    command = """
        import sys
        import os
        
        len = sys.stdout.write(str(os.stat('{0}')[6]).encode('utf-8'))
    """.format(
        filename
    )
    self._pyboard.enter_raw_repl()
    try:
        out = self._pyboard.exec_(textwrap.dedent(command))
        print("out", out, file=sys.stderr, flush=True)
    except PyboardError as ex:
        print("except", file=sys.stderr, flush=True)
        # Check if this is an OSError #2, i.e. file doesn't exist and
        # rethrow it as something more descriptive.
        if ex.args[2].decode("utf-8").find("OSError: [Errno 2] ENOENT") != -1:
            print("fnf", file=sys.stderr, flush=True)
            self._pyboard.exit_raw_repl()
            return "-1"
        else:
            raise ex
    self._pyboard.exit_raw_repl()
    return out.decode('utf-8')

Files.get_hash = get_hash
Files.get_size = get_size

def main(args: List[str]) -> None:
    global verbose
    start_time = time.time()

    opts = docopt(__doc__, argv=args)
    verbose = opts['--verbose']
    root = opts['PATH']
    different = opts['--different']

    chdir = opts['--chdir']
    if chdir:
        os.chdir(chdir)

    port = opts['PORT']
    print('Connecting to {}'.format(port), file=sys.stderr)
    board = Pyboard(port)
    files = Files(board)
    rel_root = os.path.relpath(root, os.getcwd())

    wait_for_board()

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
            make_dirs(files, remote_dir, created_cache)
        with open(local_path, 'rb') as fd:
            if different:
                raw = fd.read()
                local_file_size = str(os.stat(local_path)[6])
                local_file_hash = sha1(raw).hexdigest()

                remote_file_size = files.get_size(remote_path)
                print("Size", remote_file_size, local_file_size, file=sys.stderr, flush=True)
                if local_file_size == remote_file_size:
                    print("Size same", file=sys.stderr, flush=True)
                    remote_file_hash = files.get_hash(remote_path)
                    if remote_file_hash == local_file_hash:
                        print("File Identical", file=sys.stderr, flush=True)
                    else:
                        print("HASH", local_file_hash, remote_file_hash, file=sys.stderr, flush=True)
                        print("Files different...", file=sys.stderr, flush=True)
                        wait_for_board()
                        files.put(remote_path, raw)
                else:
                    print("Sizes different... Uploading", file=sys.stderr, flush=True)
                    wait_for_board()
                    files.put(remote_path, raw)
            else:
                files.put(remote_path, fd.read())

    print('Soft reboot', file=sys.stderr, flush=True)
    soft_reset(board)
    print("--- %s seconds ---" % (time.time() - start_time), file=sys.stderr, flush=True)


def make_dirs(files: Files, path: str,
              created_cache: Set[str] = None) -> None:
    """Make all the directories the specified relative path consists of."""
    if path == '.':
        return
    if created_cache is None:
        created_cache = set()
    parent = os.path.dirname(path)
    if parent and parent not in created_cache:
        make_dirs(files, parent, created_cache)
    with suppress(DirectoryExistsError):
        posix_path = path.replace(os.path.sep, '/')
        files.mkdir(posix_path)
        created_cache.add(path)


def soft_reset(board: Pyboard) -> None:
    """Perform soft-reset of the ESP8266 board."""
    board.serial.write(b'\x03\x04')


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


def wait_for_board() -> None:
    """Wait for some ESP8266 devices to become ready for REPL commands."""
    time.sleep(0.5)


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
