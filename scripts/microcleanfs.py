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


"""Remove the contents of the file system on a MicroPython device, skipping boot.py
"""
import os


def delete_recursive(folder):
    for fileInfo in os.ilistdir(folder):
        name = folder + fileInfo[0]
        if fileInfo[1] == 16384:
            print("Scanning " + name, end="\r\n")
            delete_recursive(name + '/')
            os.rmdir(name)
        else:
            if name == 'boot.py':
                print("Skipping boot.py", end="\r\n")
            else:
                print("Deleting " + name, end="\r\n")
                os.remove(name)


delete_recursive('')
