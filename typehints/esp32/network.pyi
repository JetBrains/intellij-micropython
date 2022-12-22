"""network configuration

This module provides network drivers and routing configuration. To use this
module, a MicroPython variant/build with network capabilities must be installed.
Network drivers for specific hardware are available within this module and are
used to configure hardware network interface(s). Network services provided
by configured interfaces are then available for use via the :mod:`usocket`
module.

For example::

    # connect/ show IP config a specific network interface
    # see below for examples of specific drivers
    import network
    import utime
    nic = network.Driver(...)
    if not nic.isconnected():
        nic.connect()
        print("Waiting for connection...")
        while not nic.isconnected():
            utime.sleep(1)
    print(nic.ifconfig())

    # now use usocket as usual
    import usocket as socket
    addr = socket.getaddrinfo('micropython.org', 80)[0][-1]
    s = socket.socket()
    s.connect(addr)
    s.send(b'GET / HTTP/1.1\r\nHost: micropython.org\r\n\r\n')
    data = s.recv(1000)
    s.close()
"""

from typing import overload, Optional, List, Tuple, Union, Any, Final

STA_IF: Final[int] = ...
AP_IF: Final[int] = ...

@overload
def phy_mode() -> int:
    """Get the PHY mode."""
    ...


@overload
def phy_mode(mode: int) -> None:
    """Set the PHY mode.

    The possible modes are defined as constants:
    * ``MODE_11B`` -- IEEE 802.11b,
    * ``MODE_11G`` -- IEEE 802.11g,
    * ``MODE_11N`` -- IEEE 802.11n.
    """
    ...


class WLAN:
    def __init__(self, interface_id: int) -> None:
        """Create a WLAN network interface object. Supported interfaces are
        ``network.STA_IF`` (station aka client, connects to upstream WiFi access
        points) and ``network.AP_IF`` (access point, allows other WiFi clients to
        connect). Availability of the methods below depends on interface type.
        For example, only STA interface may `connect()` to an access point.
        """
        ...

    @overload
    def active(self) -> bool:
        """Query current state of the interface."""
        ...

    @overload
    def active(self, is_active: bool) -> None:
        """Activate ("up") or deactivate ("down") network interface."""
        ...

    def connect(self, ssid: Optional[Union[bytes, str]] = None,
                password: Optional[Union[bytes, str]] = None, *,
                bssid: Optional[Union[bytes, str]] = None) -> None:
        """Connect to the specified wireless network, using the specified password.
        If *bssid* is given then the connection will be restricted to the
        access-point with that MAC address (the *ssid* must also be specified
        in this case).
        """
        ...

    def disconnect(self) -> None:
        """Disconnect from the currently connected wireless network."""
        ...

    def scan(self) -> List[Tuple[bytes, bytes, int, int, int, int]]:
        """Scan for the available wireless networks.

        Scanning is only possible on STA interface. Returns list of tuples with
        the information about WiFi access points:

            (ssid, bssid, channel, RSSI, authmode, hidden)

        *bssid* is hardware address of an access point, in binary form, returned as
        bytes object. You can use `ubinascii.hexlify()` to convert it to ASCII form.

        There are five values for authmode:

            * 0 -- open
            * 1 -- WEP
            * 2 -- WPA-PSK
            * 3 -- WPA2-PSK
            * 4 -- WPA/WPA2-PSK

        and two for hidden:

            * 0 -- visible
            * 1 -- hidden
        """
        ...

    def status(self) -> int:
        """Return the current status of the wireless connection.

        The possible statuses are defined as constants:

            * ``STAT_IDLE`` -- no connection and no activity,
            * ``STAT_CONNECTING`` -- connecting in progress,
            * ``STAT_WRONG_PASSWORD`` -- failed due to incorrect password,
            * ``STAT_NO_AP_FOUND`` -- failed because no access point replied,
            * ``STAT_CONNECT_FAIL`` -- failed due to other problems,
            * ``STAT_GOT_IP`` -- connection successful.
        """
        ...

    def isconnected(self) -> bool:
        """In case of STA mode, returns ``True`` if connected to a WiFi access
        point and has a valid IP address.  In AP mode returns ``True`` when a
        station is connected. Returns ``False`` otherwise.
        """
        ...

    @overload
    def ifconfig(self) -> Tuple[str, str, str, str]:
        """Get IP-level network interface parameters: IP address, subnet mask,
        gateway and DNS server.
        """
        ...

    @overload
    def ifconfig(self, ip: str, subnet: str, gateway: str, dns: str) -> None:
        """Get/set IP-level network interface parameters: IP address, subnet mask,
        gateway and DNS server.
        """
        ...

    @overload
    def config(self, param: str) -> Any:
        """Get general network interface parameters."""
        ...

    @overload
    def config(self, **kwargs: Any) -> None:
        """Get or set general network interface parameters. These methods allow to work
        with additional parameters beyond standard IP configuration (as dealt with by
        `wlan.ifconfig()`). These include network-specific and hardware-specific
        parameters. For setting parameters, keyword argument syntax should be used,
        multiple parameters can be set at once. For querying, parameters name should
        be quoted as a string, and only one parameter can be queries at time::

            # Set WiFi access point name (formally known as ESSID) and WiFi channel
            ap.config(essid='My AP', channel=11)
            # Query params one by one
            print(ap.config('essid'))
            print(ap.config('channel'))

        Following are commonly supported parameters (availability of a specific parameter
        depends on network technology type, driver, and `MicroPython port`).

        =============  ===========
        Parameter      Description
        =============  ===========
        mac            MAC address (bytes)
        essid          WiFi access point name (string)
        channel        WiFi channel (integer)
        hidden         Whether ESSID is hidden (boolean)
        authmode       Authentication mode supported (enumeration, see module constants)
        password       Access password (string)
        dhcp_hostname  The DHCP hostname to use
        =============  ===========
        """
        ...


STA_IF: int
AP_IF: int


STAT_IDLE: int
STAT_CONNECTING: int
STAT_WRONG_PASSWORD: int
STAT_NO_AP_FOUND: int
STAT_CONNECT_FAIL: int
STAT_GOT_IP: int


MODE_11B: int
MODE_11G: int
MODE_11N: int

