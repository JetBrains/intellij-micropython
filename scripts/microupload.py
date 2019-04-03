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

import time
import sys
import os
from contextlib import suppress
from typing import List, Iterable, TypeVar, Sequence, Set

def main(args: List[str]) -> None:

    from mp.mpfshell import main
    # print(args[2])
    sys.argv = [args[0], '-c', 'open', args[4] + ';', 'lcd', args[2] + ';', 'runfile', os.path.basename(args[5]) + ';', '-n', '--nohelp']
    # print(sys.argv)
    main()

if __name__ == '__main__':
    main(sys.argv)
