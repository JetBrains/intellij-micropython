# MicroPython Plugin for IntelliJ and PyCharm

[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

The plugin for [MicroPython](http://micropython.org/) devices in [IntelliJ IDEA](https://www.jetbrains.com/idea/) and
[PyCharm](https://www.jetbrains.com/pycharm/).

![Micro:bit development in IntelliJ](media/screenshot.png)


## Supported Devices

The plugin supports these devices:

* [BBC Micro:bit](https://github.com/vlasovskikh/intellij-micropython/wiki/BBC-Micro%3Abit)
* [ESP8266](https://github.com/vlasovskikh/intellij-micropython/wiki/ESP8266)
* [PyBoard](https://github.com/vlasovskikh/intellij-micropython/wiki/Pyboard)

This plugin is still in its early days. It will support more MicroPython devices and more
device-specific and MicroPython-specific modules eventually. We are interested in your
contributions to the project. Feel free to open issues and send pull requests!


## Features


### Code insight for MicroPython modules

* Context-aware code completion and documentation

![Code completion](media/code-completion.png)

* Syntax checking and type checking

![Type checking](media/type-checking.png)


### Run code on MicroPython devices

* Flash Python files to devices

![Run](media/run.png)

* MicroPython REPL

![REPL](media/repl.png)


## Requirements

* IntelliJ 2017.2+ or PyCharm 2017.2+
* Python 3.5+


## Installation

Install the "MicroPython" plugin from your IDE settings.

The setup steps differ for IntelliJ and PyCharm:

* IntelliJ: Add the MicroPython facet to a Python module in your project structure
* PyCharm: Enable MicroPython support in "File | Settings | Languages & Frameworks | MicroPython"


## Known Issues

* REPL doesn't start on Windows ([#12](https://github.com/vlasovskikh/intellij-micropython/issues/12))
* References to `pyb` module are marked as unresolved ([#16](https://github.com/vlasovskikh/intellij-micropython/issues/16))
* References to some modules from the MicroPython standard library are marked as unresolved


## Source Code

The plugin is written in [Kotlin](https://kotlinlang.org/). It's a new JVM language by JetBrains, the makers of
IntelliJ and PyCharm. Google recently selected Kotlin as an officially supported language for Android development.

The steps for setting up the development environment:

1. Check out this project from GitHub
2. Create a new project from existing sources in IntelliJ 2017.2 or newer

To just run the development version use `./gradlew clean runIde` from the command line.

Contributions are welcome!


## License

The plugin is licensed under the terms of the Apache 2 license.
