"""
This module implements a subset of the corresponding CPython module, as
described below. For more information, refer to the original CPython
documentation: ``time``.

The ``utime`` module provides functions for getting the current time and
date, measuring time intervals, and for delays.

Time Epoch: Unix port uses standard for POSIX systems epoch of **1970-01-01 00:00:00 UTC**.
However, embedded ports use epoch of **2000-01-01 00:00:00 UTC**.

Maintaining actual calendar date/time: This requires a Real Time Clock (RTC). On systems
with underlying OS (including some RTOS), an RTC may be implicit. Setting and maintaining
actual calendar time is responsibility of OS/RTOS and is done outside of MicroPython, it
just uses OS API to query date/time. On baremetal ports however system time depends on
``machine.RTC() object``. The current calendar time may be set using
``machine.RTC().datetime(tuple)`` function, and maintained by following means:

* By a backup battery (which may be an additional, optional component for a particular board).
* Using networked time protocol (requires setup by a port/user).
* Set manually by a user on each power-up (many boards then maintain RTC time across hard resets, though some may require setting it again in such case).

If actual calendar time is not maintained with a system/MicroPython RTC,
functions below which require reference to current absolute time may
behave not as expected.
"""

from typing import Optional


def sleep_ms(ms: int) -> None:
    """
    Delay for given number of milliseconds, should be positive or 0.

    :param ms: Delay in milliseconds
    :type ms: int
    """
    ...

def sleep_us(us: int) -> None:
    """
    Delay for given number of microseconds, should be positive or 0.

    :param us: Delay in microseconds
    :type us: int
    """
    ...

def ticks_ms() -> int:
    """Returns an increasing millisecond counter with an arbitrary reference
    point, that wraps around after some value.

    The wrap-around value is not explicitly exposed, but we will refer to it
    as TICKS_MAX to simplify discussion. Period of the values is
    TICKS_PERIOD = TICKS_MAX + 1. TICKS_PERIOD is guaranteed to be a power
    of two, but otherwise may differ from port to port. The same period value
    is used for all of ticks_ms(), ticks_us(), ticks_cpu() functions
    (for simplicity). Thus, these functions will return a value in range
    [0 .. TICKS_MAX], inclusive, total TICKS_PERIOD values. Note that only
    non-negative values are used. For the most part, you should treat values
    returned by these functions as opaque. The only operations available for
    them are ticks_diff() and ticks_add() functions described below.

    Note: Performing standard mathematical operations (+, -) or relational
    operators (<, <=, >, >=) directly on these value will lead to invalid
    result. Performing mathematical operations and then passing their results
    as arguments to ticks_diff() or ticks_add() will also lead to invalid
    results from the latter functions.
    """
    ...

def ticks_us() -> int:
    """Returns an increasing microsecond counter with an arbitrary reference
    point, that wraps around after some value.

    The wrap-around value is not explicitly exposed, but we will refer to it
    as *TICKS_MAX* to simplify discussion. Period of the values is
    *TICKS_PERIOD = TICKS_MAX + 1*. *TICKS_PERIOD* is guaranteed to be a power
    of two, but otherwise may differ from port to port. The same period value
    is used for all of ``ticks_ms()``, ``ticks_us()``, ``ticks_cpu()`` functions
    (for simplicity). Thus, these functions will return a value in range
    *[0 .. TICKS_MAX]*, inclusive, total *TICKS_PERIOD* values. Note that only
    non-negative values are used. For the most part, you should treat values
    returned by these functions as opaque. The only operations available for
    them are ticks_diff() and ticks_add() functions described below.

    Note: Performing standard mathematical operations (+, -) or relational
    operators (<, <=, >, >=) directly on these value will lead to invalid
    result. Performing mathematical operations and then passing their results
    as arguments to ticks_diff() or ticks_add() will also lead to invalid
    results from the latter functions."""
    ...

def ticks_cpu() -> int:
    """Similar to ``ticks_ms()`` and ``ticks_us()``, but with the highest possible
    resolution in the system. This is usually CPU clocks, and that’s why the
    function is named that way. But it doesn’t have to be a CPU clock, some
    other timing source available in a system (e.g. high-resolution timer)
    can be used instead. The exact timing unit (resolution) of this function
    is not specified on ``utime`` module level, but documentation for a specific
    port may provide more specific information. This function is intended for
     very fine benchmarking or very tight real-time loops. Avoid using it in
     portable code."""

def localtime(secs: Optional[int]) -> Tuple:
    """
    Convert a time expressed in seconds since the Epoch (see above) into an
    8-tuple which contains: (year, month, mday, hour, minute, second, weekday,
    yearday). If secs is not provided or None, then the current time from the RTC is used.

    Tuple constraints:
    * **year** includes the century (for example 2014).
    *

    :param secs:
    :return:
    """
    ...

def localtime(secs: Optional[int] = None) -> Tuple:
    """
    Convert a time expressed in seconds since the Epoch (see above) into an
    8-tuple which contains: (year, month, mday, hour, minute, second, weekday,
    yearday). If secs is not provided or None, then the current time from the
    RTC is used.

    Tuple constraints:

    * **year** includes the century (for example 2014).
    * **month** is *1-12*
    * **mday** is *1-31*
    * **hour** is *0-23*
    * **minute** is *0-59*
    * **second** is *0-59*
    * **weekday** is *0-6* for *Mon-Sun*
    * **yearday** is *1-366*

    :param secs: Specific moment in time, expressed in seconds since Epoch.
    :return: Tuple with decoded time information.
    :rtype: tuple
    """
    ...

def mktime(time: tuple) -> int:
    """
    This is inverse function of localtime. It’s argument is a full 8-tuple
    which expresses a time as per localtime. It returns an integer which
    is the number of seconds since Jan 1, 2000.

    :param time: Full 8-tuple which expresses a time as per localtime.
    :return: Amount of seconds since Epoch.
    :rtype: int
    """
    ...

def sleep(seconds: int) -> None:
    """
    Sleep for the given number of seconds. Some boards may accept seconds
    as a floating-point number to sleep for a fractional number of seconds.
    Note that other boards may not accept a floating-point argument, for
    compatibility with them use ``sleep_ms()`` and ``sleep_us()`` functions.

    :param seconds: Amount of time to sleep for.
    """
    ...

def time() -> int:
    """
    Returns the number of seconds, as an integer, since the Epoch, assuming
    that underlying RTC is set and maintained as described above. If an RTC
    is not set, this function returns number of seconds since a port-specific
    reference point in time (for embedded boards without a battery-backed RTC,
    usually since power up or reset). If you want to develop portable
    MicroPython application, you should not rely on this function to provide
    higher than second precision. If you need higher precision, use ``ticks_ms()``
    and ``ticks_us()`` functions, if you need calendar time, ``localtime()``
    without an argument is a better choice.

    In CPython, this function returns number of seconds since Unix epoch,
    **1970-01-01 00:00 UTC**, as a floating-point, usually having microsecond
    precision. With MicroPython, only Unix port uses the same Epoch, and if
    floating-point precision allows, returns sub-second precision. Embedded
    hardware usually doesn’t have floating-point precision to represent both
    long time ranges and subsecond precision, so they use integer value with
    second precision. Some embedded hardware also lacks battery-powered RTC,
    so returns number of seconds since last power-up or from other relative,
    hardware-specific point (e.g. reset).

    :return: Number of seconds, as an integer, since the Epoch.
    """
    ...