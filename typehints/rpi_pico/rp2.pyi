"""
Raspberry Pi Pico specific micropython support.

These methods are not fully documented and this is a best effort to produce the interface

"""

from typing import Optional, Callable

from machine import Pin


class Flash:
    """
    Low level access to flash device
    """

    def ioctl(self):
        pass

    def readblocks(self):
        pass

    def writeblocks(self):
        pass


class irq:
    """
    IRQ definition
    """

    def flags(self) -> int:
        """
        IRQ flags
        """

    def trigger(self):
        """
        Trigger IRQ
        """


class PIO:
    IN_LOW: int = 0
    IN_HIGH: int = 1
    OUT_LOW: int = 2
    OUT_HIGH: int = 3
    SHIFT_LEFT: int = 0
    SHIFT_RIGHT: int = 1
    IRQ_SM0: int = 256
    IRQ_SM1: int = 512
    IRQ_SM2: int = 1024
    IRQ_SM3: int = 2048

    def __init__(self, num: int):
        pass

    def irq(self, callback: Optional[Callable[["PIO"], None]]) -> irq:
        pass


class StateMachine:
    """
    Instantiate a state machine with a program.
    """
    def __init__(self, num: int, prog: list, freq: int = None, set_base: Pin = None) -> None:
        pass

    def active(self) -> bool:
        """
        This state machine is active
        """

    def init(self, prog: list):
        """
        Initialise and start a PIO program
        """

    def irq(self) -> PIO.irq:
        pass

    def put(self, data: bytes):
        """
        Send data to PIO program
        """


class PIOASMError(Exception):
    pass


def asm_pio(**kwargs) -> list:
    """
    Assemble a PIO program
    """
