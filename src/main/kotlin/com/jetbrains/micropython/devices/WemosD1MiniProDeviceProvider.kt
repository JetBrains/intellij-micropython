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
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author lensvol
 */
class WemosD1MiniProDeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "WEMOS D1 mini Pro"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0x10C4, 0xEA60))

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/WEMOS-D1-mini-Pro"

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython", "esp8266"))
  }

  override val packageRequirements: List<PyRequirement> by lazy {
    PyRequirement.fromText("""pyserial>=3.3,<3.4""")
  }
}