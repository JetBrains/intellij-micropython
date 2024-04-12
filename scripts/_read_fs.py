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


class F:
    f_list = [""]

    # noinspection PyMethodMayBeStatic
    def process_file(self, folder, file):
        return folder + file[0] + ",F," + str(file[3])

    def process_dir(self, folder, sub_folder):
        self.f_list.append(folder + sub_folder[0] + "/") if sub_folder[1] == 16384 else 0
        return folder + sub_folder[0] + ",D"

    def dir(self):
        for d in self.f_list:
            for f in os.ilistdir(d):
                s = self.process_dir(d, f) if f[1] == 16384 else self.process_file(d, f)
                print(s)


F().dir()
