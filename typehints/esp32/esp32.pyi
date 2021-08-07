"""esp32 methods

This module provides methods that are applicable to an ESP32. To use this
module, a MicroPython variant/build based on an esp32 must be installed.

"""

from typing import overload, Optional, List, Tuple, Union, Any, Literal

WAKEUP_ALL_LOW = ... #type: int
WAKEUP_ANY_HIGH = ... #type: int

HEAP_DATA = ... #type: int
HEAP_EXEC = ... #type: int

def wake_on_touch(wake: int) -> None:
    """Configure whether or not a touch will wake the device from sleep. wake should be a boolean value.
    """
    ...

def wake_on_ext0(pin: Pin, level: int) -> None:
    """Configure how EXT0 wakes the device from sleep. pin can be None or a valid Pin object. level should be
    esp32.WAKEUP_ALL_LOW or esp32.WAKEUP_ANY_HIGH.
    """
    ...

def wake_on_ext1(pin: Pin, level: int) -> None:
    """Configure how EXT1 wakes the device from sleep. pin can be None or a valid Pin object. level should be
    esp32.WAKEUP_ALL_LOW or esp32.WAKEUP_ANY_HIGH.
    """
    ...

def raw_temperature() -> int:
    """Read the raw value of the internal temperature sensor, returning an integer.
    """
    ...

def hall_sensor() -> int:
    """Read the raw value of the internal Hall sensor, returning an integer.
    """
    ...

def idf_heap_info(capabilities: int) -> List:
    """Returns information about the ESP-IDF heap memory regions. One of them contains the MicroPython heap
    and the others are used by ESP-IDF, e.g., for network buffers and other data. This data is useful to get
    a sense of how much memory is available to ESP-IDF and the networking stack in particular. It may shed
    some light on situations where ESP-IDF operations fail due to allocation failures. The information
    returned is not useful to troubleshoot Python allocation failures, use micropython.mem_info() instead.
    The capabilities parameter corresponds to ESP-IDF MALLOC_CAP_XXX values but the two most useful ones
    are predefined as esp32.HEAP_DATA for data heap regions and esp32.HEAP_EXEC for executable regions as
    used by the native code emitter.
    The return value is a list of 4-tuples, where each 4-tuple corresponds to one heap and contains: the total
    bytes, the free bytes, the largest free block, and the minimum free seen over time.
    """
    ...

class Partition(Object):
    BOOT = 0 #type: int
    RUNNING = 1 #type: int
    TYPE_APP = 0 #type: int
    TYPE_DATA = 1 #type: int

    def __init__(self, id: Union[str, int]) -> None:
        """Create an object representing a partition. id can be a string which is the label of the partition
        to retrieve, or one of the constants: BOOT or RUNNING.
        """
        ...

    @classmethod
    def find(cls, type: int = TYPE_APP, subtype: int = 255, label: Optional[str] = None) -> Object:
        """Find a partition specified by type, subtype and label. Returns a (possibly empty) list of Partition
        objects. Note: subtype=0xff matches any subtype and label=None matches any label.
        """
        ...

    def info(self) -> Tuple[int, int, int, int, str, bool]:
        """Returns a 6-tuple (type, subtype, addr, size, label, encrypted).
        """
        ...

    @overload
    def readblocks(self, block_num: int, buf: bytearray) -> None:
        """block_num * 4096 will be the starting address of read
        """
        ...

    @overload
    def readblocks(self, block_num: int, buf: bytearray, offset:int) -> None:
        """block_num * 4096 + offset will be the starting address of read
        """
        ...

    @overload
    def writeblocks(self, block_num: int, buf: bytearray) -> None:
        """block_num * 4096 will be the starting address of write
        """
        ...

    @overload
    def writeblocks(self, block_num: int, buf: bytearray, offset: int) -> None:
        """block_num * 4096 + offset will be the starting address of write
        """
        ...

    def ioctl(self, cmd: int, arg: any) -> None:
        """These methods implement the simple and extended block protocol defined by
        uos.AbstractBlockDev.
        """

    def set_boot(self) -> None:
        """Sets the partition as the boot partition.
        """
        ...

    def get_next_update(self) -> None:
        """Gets the next update partition after this one, and returns a new Partition
        object. Typical usage is Partition(Partition.RUNNING).get_next_update() which
        returns the next partition to update given the current running one.
        """
        ...

    @classmethod
    def mark_app_valid_cancel_rollback(cls) -> None:
        """Signals that the current boot is considered successful. Calling
        mark_app_valid_cancel_rollback is required on the first boot of a new partition
        to avoid an automatic rollback at the next boot. This uses the ESP-IDF "app rollback"
        feature with "CONFIG_BOOTLOADER_APP_ROLLBACK_ENABLE' and an OSError(-261) is raised
        if called on firmware that doesn't have the feature enabled. It is OK to call
        mark_app_valid_cancel_rollback on every boot and it is not necessary when booting
        firmare that was loaded using esptool.
        """
        ...

class RMT(Object):
    def __init__(self, channel: int, *, pin: Pin = None, clock_div: Optional[int] = 8,
                 carrier_freq: Optional[int] = 0,
                 carrier_duty_cycle: Optional[int] = 50) -> None:
        """Create an RMT channel
        """
        ...

    def deinit(self) -> None:
        """Release an RMT channel
        """
        ...

    def source_freq(self) -> int:
        """Get the source frequency
        """
        ...

    def clock_div(self) -> int:
        """Return the clock divider. Not that the channel resolution is 1 / (source_freq / clock_div).
        """
        ...

    def wait_done(self, timeout: Optional[int] = 0) -> bool:
        """Returns True if the channel is currently transmitting a stream of pulses started with a call
        to write_pulses.
        """
        ...

    def loop(self, enable_loop: bool) -> None:
        """Configure looping on the channel. enable_loop is bool, set to True to enable looping on the next
        call to RMT.write_pulses. If called with False while a looping stream is currently being transmitted
        then the current set of pulses will be completed before transmission stops.
        """
        ...

    def write_pulses(self, pulses: List[int], start: Optional[int] = 1):
        """Begin sending pulses, a list or tuple. defining the stream of pulses.
        The length of each pulse is defined by a number to be multiplied by the
        channel resolution (1 / (source_freq / clock_div)). start defines whether the stream starts at 0 or 1.

        If transmission of a stream is currently in progress then this method will block
        until transmission of that stream has ended before beginning sending pulses.

        If looping is enabled with RMT.loop, the stream of pulses will be repeated indefinitely.
        Further calls to RMT.write_pulses will end the previous stream - blocking until the last
        set of pulses has been transmitted - before starting the next stream.
        """
        ...

class ULP(Object):

    def __init__(self) -> None:
        """This class provides access to the Ultra-Low-Power co-processor.
        """
        ...

    def set_wakeup_period(self, period_index: int, period_us: int) -> None:
        """Set the wake-up period.
        """
        ...

    def load_binary(self, load_addr: int, program_binary: bytearray) -> None:
        """Load a program_binary into the ULP at the given load_addr.
        """
        ...

    def run(self, entry_point: int) -> None:
        """Start the ULP running at the given entry_point.
        """
        ...

class NVS(Object):

    def __init__(self, namespace: str) -> None:
        """Create an object providing access to a namespace (which is automatically created if not present).
        """
        ...

    def set_i32(self, key: str, value: int) -> None:
        """Sets a 32-bit signed integer value for the specified key. Remember to call commit!
        """
        ...

    def get_i32(self, key: str) -> int:
        """Returns the signed integer value for the specified key. Raises an OSError if the key does not
        exist or has a different type.
        """
        ...

    def set_blob(self, key: str, value: Union[byte, bytearray, str]) -> None:
        """Sets a binary blob value for the specified key. The value passed in must support the buffer
        protocol, e.g. bytes, bytearray, str. (Note that esp-idf distinguishes blobs and strings, this
        method always writes a blob even if a string is passed in as value.) Remember to call commit!
        """
        ...

    def get_blob(self, key: str, buffer: bytearray) -> None:
        """Reads the value of the blob for the specified key into the buffer, which must be a bytearray.
        Returns the actual length read. Raises an OSError if the key does not exist, has a different
        type, or if the buffer is too small.
        """
        ...

    def erase_key(self, key: str) -> None:
        """Erases a key-value pair.
        """
        ...

    def commit(self) -> None:
        """Commits changes made by set_xxx methods to flash.
        """
