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
    microcleanfs PORT
"""
import traceback
import time
import sys
import os
from contextlib import suppress
from typing import List, Iterable, TypeVar, Sequence, Set

def main(args: List[str]) -> None:

    from mp.mpfshell import MpFileShell
    # print(args)
    # sys.arg
    mpfs = MpFileShell(True, True, True, True)
    mpfs.do_open(args[1])
    files = mpfs.fe.ls(add_details=True)
    print('Currently has internal files ', files)
    for file, type in files:
        mpfs.fe.rm(file)
    files = mpfs.fe.ls(add_details=True)
    print('The execution result', files if 0 != len(files) else 'success')

if __name__ == '__main__':
    try:
        main(sys.argv)
        input("Press ENTER to exit")
    except Exception:
        sys.stderr.write(traceback.format_exc())
        input("Press ENTER to continue")
        sys.exit(1)
