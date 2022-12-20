from machine import Pin
from typing import Optional
from typing import Final

HEAP_DATA: Final[int] = ...
"""Used in ``idf_heap_info``."""

HEAP_EXEC: Final[int] = ...
"""Used in ``idf_heap_info``."""

WAKEUP_ALL_LOW: Final[int] = ...
"""Selects the wake level for pins."""

WAKEUP_ANY_HIGH: Final[int] = ...
"""Selects the wake level for pins."""

def wake_on_touch(wake: bool):
    """
    :param wake: Configure whether or not a touch will wake the device from sleep.
    """

def wake_on_ulp(wake: bool):
    """
    :param wake: Configure whether or not the Ultra-Low-Power co-processor can wake the device from sleep.
    """

def wake_on_ext0(pin: Pin, level: Optional[int]):
    """
    Configure how EXT0 wakes the device from sleep.

    :param pin: None or a valid Pin object.
    :param level: ``esp32.WAKEUP_ALL_LOW`` or ``esp32.WAKEUP_ANY_HIGH``.
    """

def wake_on_ext1(pin: Pin, level: Optional[int]):
    """
    Configure how EXT1 wakes the device from sleep.

    :param pin: None or a valid Pin object.
    :param level: ``esp32.WAKEUP_ALL_LOW`` or ``esp32.WAKEUP_ANY_HIGH``.
    """

def gpio_deep_sleep_hold(enable: bool):
    """
    :param enable: Whether non-RTC GPIO pin configuration is retained during deep-sleep mode for held pads.
    """

def raw_temperature() -> int:
    """
    :return: The raw value of the internal temperature sensor.
    """

def hall_sensor() -> int:
    """
    :return: The raw value of the internal Hall sensor.
    """

def idf_heap_info(capabilities: Optional[int]) -> list:
    """
    Returns information about the ESP-IDF heap memory regions. One of them contains the MicroPython heap and the others
    are used by ESP-IDF, e.g., for network buffers and other data. This data is useful to get a sense of how much
    memory is available to ESP-IDF and the networking stack in particular. It may shed some light on situations where
    ESP-IDF operations fail due to allocation failures. The information returned is not useful to troubleshoot Python
    allocation failures, use ``micropython.mem_info()`` instead.

    The capabilities parameter corresponds to ESP-IDF’s ``MALLOC_CAP_XXX`` values but the two most useful ones are
    predefined as ``esp32.HEAP_DATA`` for data heap regions and ``esp32.HEAP_EXEC`` for executable regions as used by
    the native code emitter.

    The return value is a list of 4-tuples, where each 4-tuple corresponds to one heap and contains: the total bytes,
    the free bytes, the largest free block, and the minimum free seen over time.
    """

class Partition:
    """
    This class gives access to the partitions in the device’s flash memory and includes methods to
    enable over-the-air (OTA) updates.
    """

    BOOT: Final[str] = ...
    """Used in ``Partition`` constructor - The partition that will be booted at the next reset."""

    RUNNING: Final[str] = ...
    """Used in ``Partition`` constructor - The currently running partition."""

    TYPE_APP: Final[int] = ...
    """
    Used in ``Partition.find``
    - for bootable firmware partitions (typically labelled ``factory``, ``ota_0``, ``ota_1``)
    """

    TYPE_DATA: Final[int] = ...
    """
    Used in Partition.find
    - for other partitions, e.g. ``nvs``, ``otadata``, ``phy_init``, ``vfs``.
    """

    def __init__(self, id: str, block_size=4096, /):
        """
        Create an object representing a partition.

        :param id: Can be a string which is the label of the partition to retrieve or one of the
        constants: ``BOOT`` or ``RUNNING``.
        :param block_size: Specifies the byte size of an individual block.
        """

    @classmethod
    def find(self, type=TYPE_APP, subtype=0xff, label=None, block_size=4096) -> list:
        """
        Find a partition specified by type, subtype and label.

        Note: ``subtype=0xff`` matches any subtype and ``label=None`` matches any label.

        :param block_size: Specifies the byte size of an individual block used by the returned objects.
        :return: A (possibly empty) list of Partition objects.
        """

    def info(self) -> tuple:
        """
        :return: A 6-tuple ``(type, subtype, addr, size, label, encrypted)``.
        """

    def readblocks(self, block_num, buf):

    def readblocks(self, block_num, buf, offset):

    def writeblocks(self, block_num, buf):

    def writeblocks(self, block_num, buf, offset):

    def ioctl(self, cmd, arg):
        """These methods implement the simple and extended block protocol defined by ``os.AbstractBlockDev``."""

    def set_boot(self):
        """Sets the partition as the boot partition."""

    def get_next_update(self):
        """
        Gets the next update partition after this one, and returns a new Partition object.

        Typical usage is ``Partition(Partition.RUNNING).get_next_update()``.

        :return: The next partition to update given the current running one.
        """

    def mark_app_valid_cancel_rollback(self):
        """
        Signals that the current boot is considered successful. Calling ``mark_app_valid_cancel_rollback`` is required
        on the first boot of a new partition to avoid an automatic rollback at the next boot. This uses the
        ESP-IDF “app rollback” feature with “CONFIG_BOOTLOADER_APP_ROLLBACK_ENABLE” and an ``OSError(-261)`` is raised
        if called on firmware that doesnt have the feature enabled. It is OK to call ``mark_app_valid_cancel_rollback``
        on every boot and it is not necessary when booting firmware that was loaded using esptool.
        """

class RMT:
    """
    This class provides access to one of the eight RMT channels.

    WARNING: The current MicroPython RMT implementation lacks some features, most notably receiving pulses. RMT should
    be considered a beta feature and the interface may change in the future.
    """

    def __init__(self, channel: int, *, pin=None, clock_div=8, idle_level=False, tx_carrier=None):
        """
        :param channel: Identifies which RMT channel (0-7) will be configured.
        :param pin: Configures which Pin is bound to the RMT channel
        :param clock_div: An 8-bit clock divider that divides the source clock (80MHz) to the RMT channel allowing the
        resolution to be specified.
        :param idle_level: Specifies what level the output will be when no transmission is in progress

        To enable the transmission carrier feature, ``tx_carrier`` should be a tuple of three positive integers: carrier
        frequency, duty percent (0 to 100) and the output level to apply the carrier to (a boolean as per idle_level).
        """

    def source_freq(self):
        """
        :return: The source clock frequency. Currently the source clock is not configurable so this will always
        return 80MHz.
        """

    def clock_div(self):
        """
        :return: The clock divider. Note that the channel resolution is ``1 / (source_freq / clock_div)``.
        """

    def wait_done(self, *, timeout=0):
        """
        If the timeout keyword argument is given then block for up to this many milliseconds for transmission to
        complete.

        :return: ``True`` if the channel is idle or ``False`` if a sequence of pulses started with ``RMT.write_pulses``
        is being transmitted.
        """

    def loop(self, enable_loop: bool):
        """
        Configure looping on the channel.

        :param enable_loop: ``True`` to enable looping on the next call to ``RMT.write_pulses``. If called with
        ``False`` while a looping sequence is currently being transmitted then the current loop iteration will be
        completed and then transmission will stop.
        """

    def write_pulses(self, duration, data=True):
        """
        Begin transmitting a sequence. There are three ways to specify this:

        Mode 1: duration is a list or tuple of durations. The optional data argument specifies the initial output level.
        The output level will toggle after each duration.

        Mode 2: duration is a positive integer and data is a list or tuple of output levels.
        duration specifies a fixed duration for each.

        Mode 3: duration and data are lists or tuples of equal length, specifying individual durations and the output
        level for each.

        Durations are in integer units of the channel resolution (as described above), between 1 and 32767 units.
        Output levels are any value that can be converted to a boolean, with ``True`` representing high voltage and
        ``False`` representing low.

        If transmission of an earlier sequence is in progress then this method will block until that transmission is
        complete before beginning the new sequence.

        If looping has been enabled with RMT.loop, the sequence will be repeated indefinitely. Further calls to this
        method will block until the end of the current loop iteration before immediately beginning to loop the new
        sequence of pulses. Looping sequences longer than 126 pulses is not supported by the hardware.
        """

    @staticmethod
    def bitstream_channel([value]) -> int:
        """
        Select which RMT channel is used by the ``machine.bitstream`` implementation.

        :param value: Can be None or a valid RMT channel number. The default RMT channel is the highest numbered one.
        Passing in None disables the use of RMT and instead selects a bit-banging implementation for
        ``machine.bitstream``. Passing in no argument will not change the channel. This function returns the current
        channel number.
        :type value: int
        """

class ULP:
    """This class provides access to the Ultra-Low-Power co-processor."""

    def set_wakeup_period(self, period_index: int, period_us):
        """Set the wake-up period."""

    def load_binary(self, load_addr, program_binary):
        """Load a program_binary into the ULP at the given load_addr."""

    def run(self, entry_point):
        """Start the ULP running at the given entry_point."""

class NVS:
    """
    This class gives access to the Non-Volatile storage managed by ESP-IDF. The NVS is partitioned into namespaces
    and each namespace contains typed key-value pairs. The keys are strings and the values may be various integer types,
    strings, and binary blobs. The driver currently only supports 32-bit signed integers and blobs.
    """

    def __init__(self, namespace: str):
        """Create an object providing access to a namespace (which is automatically created if not present)."""

    def set_i32(self, key, value):
        """Sets a 32-bit signed integer value for the specified key. Remember to call commit!"""

    def get_i32(self, key):
        """
        Returns the signed integer value for the specified key. Raises an OSError if the key does not exist or has
        a different type.
        """

    def set_blob(self, key, value):
        """
        Sets a binary blob value for the specified key. The value passed in must support the buffer protocol,
        e.g. bytes, bytearray, str. (Note that esp-idf distinguishes blobs and strings, this method always writes a
        blob even if a string is passed in as value.) Remember to call commit!
        """

    def get_blob(self, key, buffer):
        """
        Reads the value of the blob for the specified key into the buffer, which must be a bytearray.
        Returns the actual length read. Raises an OSError if the key does not exist, has a different type, or if the
        buffer is too small.
        """

    def erase_key(self, key):
        """Erases a key-value pair."""

    def commit(self):
        """Commits changes made by set_xxx methods to flash."""