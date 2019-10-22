/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.micropython.devices

import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId

/**
 * @author stefanhoelzl
 */
class PyboardDeviceProvider : MicrouploadDeviceProvider() {
  override val persistentName: String
    get() = "Pyboard"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/Pyboard"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0xF055, 0x9800))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython"))
  }
}