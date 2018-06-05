"""DHT driver

Driver enables temperature and humidity readings from the DHT-11 and DHT-22 (also know as AM2302).  Humidity is
measured as percentage of relative humidity and temperature is measured in Celsius.

"""


class DHTBase:
    def __init__(self, pin: machine.Pin) -> None:
        """
        Initialise base class for both DHT-11 and DHT-22
        :param pin: GPIO pin object providing 1-wire data signal
        """

    def measure(self) -> None:
        """
        Updates temperature and humidity measurements from the sensor and stores them them ready for reading.

        To ensure accurate results:
        * DHT-11 should have at least a 1 second wait between calls
        * DHT-22 should have at least a 2 second wait between calls
        """


class DHT11(DHTBase):
    def humidity(self) -> int:
        """
        Gets the latest humidity reading as the percentage of relative humidity as whole number.

        :return: latest humidity reading.
        """


    def temperature(self) -> int:
        """
        Gets the latest temperature reading in degrees celsius as whole number.

        :return: latest temperature reading
        """


class DHT22(DHTBase):
    def humidity(self) -> float:
        """
        Gets the latest humidity reading as the percentage of relative humidity accurate to one decimal place.

        :return: latest humidity reading
        """


    def temperature(self) -> float:
        """
        Gets the latest temperature reading in degrees celsius accurate to one decimal place.

        :return: latest temperature reading
        """
