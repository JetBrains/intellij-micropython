"""micro:bit Micropython API

Everything directly related to interacting with the hardware lives in the
`microbit` module.  For ease of use it's recommended you start all scripts
with::

    from microbit import *

The following documentation assumes you have done this.

There are a few functions available directly::

    # sleep for the given number of milliseconds.
    sleep(ms)
    # returns the number of milliseconds since the micro:bit was last switched on.
    running_time()
    # makes the micro:bit enter panic mode (this usually happens when the DAL runs
    # out of memory, and causes a sad face to be drawn on the display). The error
    # code can be any arbitrary integer value.
    panic(error_code)
    # resets the micro:bit.
    reset()

The rest of the functionality is provided by objects and classes in the
microbit module, as described below.

Note that the API exposes integers only (ie no floats are needed, but they may
be accepted).  We thus use milliseconds for the standard time unit.
"""

from . import (display as display, uart as uart, spi as spi, i2c as i2c,
               accelerometer as accelerometer, compass as compass)

from typing import Any, List, overload


def panic(n: int) -> None:
    """Enter a panic mode. Requires restart. Pass in an arbitrary integer <= 255
    to indicate a status::

        microbit.panic(255)
    """


def reset() -> None:
    """Restart the board."""


def sleep(n: int) -> None:
    """Wait for ``n`` milliseconds. One second is 1000 milliseconds, so::

        microbit.sleep(1000)

    will pause the execution for one second.  ``n`` can be an integer or
    a floating point number.
    """


def running_time() -> int:
    """Return the number of milliseconds since the board was switched on or
    restarted.
    """


def temperature() -> int:
    """Return the temperature of the micro:bit in degrees Celcius."""


class Button:
    """Represents a button.

    .. note::
        This class is not actually available to the user, it is only used by
        the two button instances, which are provided already initialized.
        """

    def is_pressed(self) -> bool:
        """Returns ``True`` if the specified button ``button`` is pressed, and
        ``False`` otherwise.
        """

    def was_pressed(self) -> bool:
        """Returns ``True`` or ``False`` to indicate if the button was pressed
        since the device started or the last time this method was called.
        """

    def get_presses(self) -> int:
        """Returns the running total of button presses, and resets this total
        to zero before returning.
        """


button_a: Button
"""A ``Button`` instance (see below) representing the left button."""

button_b: Button
"""Represents the right button."""


class MicroBitDigitalPin:
    """
    The pull mode for a pin is automatically configured when the pin changes to an
    input mode. Input modes are when you call ``read_analog`` / ``read_digital`` /
    ``is_touched``. The pull mode for these is, respectively, ``NO_PULL``,
    ``PULL_DOWN``, ``PULL_UP``. Only when in ``read_digital`` mode can you call
    ``set_pull`` to change the pull mode from the default.
    """

    NO_PULL: int = 0
    PULL_UP: int = 1
    PULL_DOWN: int = 2

    def read_digital(self) -> int:
        """Return 1 if the pin is high, and 0 if it's low."""

    def set_pull(self, value: int = (NO_PULL or PULL_UP or PULL_DOWN)) -> None:
        """Set the pull state to one of three possible values: ``pin.PULL_UP``,
        ``pin.PULL_DOWN`` or ``pin.NO_PULL`` (where ``pin`` is an instance of
        a pin). See below for discussion of default pull states.
        """

    def write_digital(self, value: int) -> None:
        """Set the pin to high if ``value`` is 1, or to low, if it is 0."""

    def write_analog(self, value: int) -> None:
        """Output a PWM signal on the pin, with the duty cycle proportional to
        the provided ``value``. The ``value`` may be either an integer or a
        floating point number between 0 (0% duty cycle) and 1023 (100% duty).
        """

    def set_analog_period(self, period: int) -> None:
        """Set the period of the PWM signal being output to ``period`` in
        milliseconds. The minimum valid value is 1ms.
        """

    def set_analog_period_microseconds(self, period: int) -> None:
        """Set the period of the PWM signal being output to ``period`` in
        microseconds. The minimum valid value is 35µs.
        """

class MicroBitAnalogDigitalPin(MicroBitDigitalPin):
    def read_analog(self) -> int:
        """Read the voltage applied to the pin, and return it as an integer
        between 0 (meaning 0V) and 1023 (meaning 3.3V).
        """

class MicroBitTouchPin(MicroBitAnalogDigitalPin):
    def is_touched(self) -> bool:
        """Return ``True`` if the pin is being touched with a finger, otherwise
        return ``False``.

        This test is done by measuring the capacitance of the pin together with
        whatever is connected to it. Human body has quite a large capacitance,
        so touching the pin gives a dramatic change in reading, which can be
        detected.
        """

pin0: MicroBitTouchPin
"""Pad 0."""

pin1: MicroBitTouchPin
"""Pad 1."""

pin2: MicroBitTouchPin
"""Pad 2."""

pin3: MicroBitAnalogDigitalPin
"""Column 1."""

pin4: MicroBitAnalogDigitalPin
"""Column 2."""

pin5: MicroBitDigitalPin
"""Button A."""

pin6: MicroBitDigitalPin
"""Row 2."""

pin7: MicroBitDigitalPin
"""Row 1."""

pin8: MicroBitDigitalPin

pin9: MicroBitDigitalPin
"""Row 3."""

pin10: MicroBitAnalogDigitalPin
"""Column 3."""

pin11: MicroBitDigitalPin
"""Button B."""

pin12: MicroBitDigitalPin

pin13: MicroBitDigitalPin
"""SPI MOSI."""

pin14: MicroBitDigitalPin
"""SPI MISO."""

pin15: MicroBitDigitalPin
"""SPI SCK."""

pin16: MicroBitDigitalPin

pin19: MicroBitDigitalPin
"""I2C SCL."""

pin20: MicroBitDigitalPin
"""I2C SDA."""


class Image:
    """The ``Image`` class is used to create images that can be displayed
    easily on the device's LED matrix. Given an image object it's possible to
    display it via the ``display`` API::

        display.show(Image.HAPPY)
    """

    HEART: Image
    HEART_SMALL: Image
    HAPPY: Image
    SMILE: Image
    SAD: Image
    CONFUSED: Image
    ANGRY: Image
    ASLEEP: Image
    SURPRISED: Image
    SILLY: Image
    FABULOUS: Image
    MEH: Image
    YES: Image
    NO: Image
    CLOCK12: Image
    CLOCK11: Image
    CLOCK10: Image
    CLOCK9: Image
    CLOCK8: Image
    CLOCK7: Image
    CLOCK6: Image
    CLOCK5: Image
    CLOCK4: Image
    CLOCK3: Image
    CLOCK2: Image
    CLOCK1: Image
    ARROW_N: Image
    ARROW_NE: Image
    ARROW_E: Image
    ARROW_SE: Image
    ARROW_S: Image
    ARROW_SW: Image
    ARROW_W: Image
    ARROW_NW: Image
    TRIANGLE: Image
    TRIANGLE_LEFT: Image
    CHESSBOARD: Image
    DIAMOND: Image
    DIAMOND_SMALL: Image
    SQUARE: Image
    SQUARE_SMALL: Image
    RABBIT: Image
    COW: Image
    MUSIC_CROTCHET: Image
    MUSIC_QUAVER: Image
    MUSIC_QUAVERS: Image
    PITCHFORK: Image
    XMAS: Image
    PACMAN: Image
    TARGET: Image
    TSHIRT: Image
    ROLLERSKATE: Image
    DUCK: Image
    HOUSE: Image
    TORTOISE: Image
    BUTTERFLY: Image
    STICKFIGURE: Image
    GHOST: Image
    SWORD: Image
    GIRAFFE: Image
    SKULL: Image
    UMBRELLA: Image
    SNAKE: Image

    ALL_CLOCKS: List[Image]
    ALL_ARROWS: List[Image]

    @overload
    def __init__(self, string: str) -> None:
        """``string`` has to consist of digits 0-9 arranged into lines,
        describing the image, for example::

            image = Image("90009:"
                          "09090:"
                          "00900:"
                          "09090:"
                          "90009")

        will create a 5×5 image of an X. The end of a line is indicated by a
        colon. It's also possible to use a newline (\n) to indicate the end of
        a line like this::

            image = Image("90009\n"
                          "09090\n"
                          "00900\n"
                          "09090\n"
                          "90009")
        """

    @overload
    def __init__(self, width: int = None, height: int = None,
                 buffer: Any = None) -> None:
        """Create an empty image with ``width`` columns and ``height`` rows.
        Optionally ``buffer`` can be an array of ``width``×``height`` integers
        in range 0-9 to initialize the image.
        """

    def width(self) -> int:
        """Return the number of columns in the image."""

    def height(self) -> int:
        """Return the numbers of rows in the image."""

    def set_pixel(self, x: int, y: int, value: int) -> None:
        """Set the brightness of the pixel at column ``x`` and row ``y`` to the
        ``value``, which has to be between 0 (dark) and 9 (bright).

        This method will raise an exception when called on any of the built-in
        read-only images, like ``Image.HEART``.
        """

    def get_pixel(self, x: int, y: int) -> int:
        """Return the brightness of pixel at column ``x`` and row ``y`` as an
        integer between 0 and 9.
        """

    def shift_left(self, n: int) -> Image:
        """Return a new image created by shifting the picture left by ``n``
        columns.
        """

    def shift_right(self, n: int) -> Image:
        """Same as ``image.shift_left(-n)``."""

    def shift_up(self, n: int) -> Image:
        """Return a new image created by shifting the picture up by ``n``
        rows.
        """

    def shift_down(self, n: int) -> Image:
        """Same as ``image.shift_up(-n)``."""

    def crop(self, x: int, y: int, w: int, h: int) -> Image:
        """Return a new image by cropping the picture to a width of ``w`` and a
        height of ``h``, starting with the pixel at column ``x`` and row
        ``y``.
        """

    def copy(self) -> Image:
        """Return an exact copy of the image."""

    def invert(self) -> Image:
        """Return a new image by inverting the brightness of the pixels in the
        source image."""

    def fill(self, value: int) -> None:
        """Set the brightness of all the pixels in the image to the
        ``value``, which has to be between 0 (dark) and 9 (bright).

        This method will raise an exception when called on any of the built-in
        read-only images, like ``Image.HEART``.
        """

    def blit(self, src: Image, x: int, y: int, w: int, h: int, xdest: int = 0,
             ydest: int = 0) -> None:
        """Copy the rectangle defined by ``x``, ``y``, ``w``, ``h`` from the
        image ``src`` into this image at ``xdest``, ``ydest``. Areas in the
        source rectangle, but outside the source image are treated as having a
        value of 0.

        ``shift_left()``, ``shift_right()``, ``shift_up()``, ``shift_down()``
        and ``crop()`` can are all implemented by using ``blit()``.

        For example, img.crop(x, y, w, h) can be implemented as::

            def crop(self, x, y, w, h):
                res = Image(w, h)
                res.blit(self, x, y, w, h)
                return res
        """

    def __repr__(self) -> str:
        """Get a compact string representation of the image."""

    def __str__(self) -> str:
        """Get a readable string representation of the image."""

    def __add__(self, other: Image) -> Image:
        """Create a new image by adding the brightness values from the two
        images for each pixel.
        """

    def __mul__(self, n: float) -> Image:
        """Create a new image by multiplying the brightness of each pixel by
        ``n``.
        """
