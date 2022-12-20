from typing import Optional
from typing import Final

LOG_NONE: Final[int] = ...
LOG_ERROR: Final[int] = ...
LOG_WARN: Final[int] = ...
LOG_INFO: Final[int] = ...
LOG_DEBUG: Final[int] = ...
LOG_VERBOSE: Final[int] = ...

def flash_size():
    """Read the total size of the flash memory."""

def flash_user_start():
    """Read the memory offset at which the user flash space begins."""

def flash_read(byte_offset, length_or_buffer):

def flash_write(byte_offset, bytes):

def flash_erase(sector_no):

def osdebug(level: Optional[int]):
    """
    Turn esp os debugging messages on or off.

    The level parameter sets the threshold for the log messages for all esp components. The log levels are defined as constants:

    * ``LOG_NONE`` – No log output
    * ``LOG_ERROR`` – Critical errors, software module can not recover on its own
    * ``LOG_WARN`` – Error conditions from which recovery measures have been taken
    * ``LOG_INFO`` – Information messages which describe normal flow of events
    * ``LOG_DEBUG`` – Extra information which is not necessary for normal use (values, pointers, sizes, etc)
    * ``LOG_VERBOSE`` – Bigger chunks of debugging information, or frequent messages which can potentially flood the output
    """
