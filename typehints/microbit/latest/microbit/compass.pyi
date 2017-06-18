"""This module lets you access the built-in electronic compass. Before using,
the compass should be calibrated, otherwise the readings may be wrong.

.. warning::

    Calibrating the compass will cause your program to pause until calibration
    is complete. Calibration consists of a little game to draw a circle on the
    LED display by rotating the device.
"""


def calibrate() -> None:
    """Starts the calibration process. An instructive message will be scrolled
    to the user after which they will need to rotate the device in order to
    draw a circle on the LED display.
    """


def is_calibrated() -> bool:
    """Returns ``True`` if the compass has been successfully calibrated, and
    returns ``False`` otherwise.
    """


def clear_calibration() -> None:
    """Undoes the calibration, making the compass uncalibrated again."""


def get_x() -> int:
    """Gives the reading of the magnetic force on the ``x`` axis, as a
    positive or negative integer, depending on the direction of the
    force.
    """


def get_y() -> int:
    """Gives the reading of the magnetic force on the ``x`` axis, as a
    positive or negative integer, depending on the direction of the
    force.
    """


def get_z() -> int:
    """Gives the reading of the magnetic force on the ``x`` axis, as a
    positive or negative integer, depending on the direction of the
    force.
    """


def heading() -> int:
    """Gives the compass heading, calculated from the above readings, as an
    integer in the range from 0 to 360, representing the angle in degrees,
    clockwise, with north as 0.
    If the compass has not been calibrated, then this will call ``calibrate``.
    """


def get_field_strength() -> int:
    """Returns an integer indication of the magnitude of the magnetic field
    around the device."""
