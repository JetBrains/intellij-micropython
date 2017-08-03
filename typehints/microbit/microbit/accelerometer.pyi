"""This object gives you access to the on-board accelerometer. The accelerometer
also provides convenience functions for detecting gestures. The
recognised gestures are: ``up``, ``down``, ``left``, ``right``, ``face up``,
``face down``, ``freefall``, ``3g``, ``6g``, ``8g``, ``shake``.
"""


from typing import Tuple


def get_x() -> int:
    """Get the acceleration measurement in the ``x`` axis, as a positive or
    negative integer, depending on the direction.
    """


def get_y() -> int:
    """Get the acceleration measurement in the ``y`` axis, as a positive or
    negative integer, depending on the direction.
    """


def get_z() -> int:
    """Get the acceleration measurement in the ``z`` axis, as a positive or
    negative integer, depending on the direction.
    """


def get_values() -> Tuple[int, int, int]:
    """Get the acceleration measurements in all axes at once, as a three-element
    tuple of integers ordered as X, Y, Z.
    """


def current_gesture() -> str:
    """Return the name of the current gesture.

    .. note::

        MicroPython understands the following gesture names: ``"up"``, ``"down"``,
        ``"left"``, ``"right"``, ``"face up"``, ``"face down"``, ``"freefall"``,
        ``"3g"``, ``"6g"``, ``"8g"``, ``"shake"``. Gestures are always
        represented as strings."""


def is_gesture(name: str) -> bool:
    """Return True or False to indicate if the named gesture is currently
    active."""


def was_gesture(name: str) -> bool:
    """Return True or False to indicate if the named gesture was active since the
    last call.
    """


def get_gestures() -> Tuple[str, ...]:
    """Return a tuple of the gesture history. The most recent is listed last.
    Also clears the gesture history before returning.
    """