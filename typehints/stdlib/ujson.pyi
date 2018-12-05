"""
This module implements a subset of the corresponding CPython module, as described below. For more information,
refer to the original CPython documentation: json.

This modules allows to convert between Python objects and the JSON data format.
"""

from typing import Optional

def dump(obj: Optional[object]) -> None:
    """
    Serialise obj to a JSON string, writing it to the given stream.
    :param obj:
    :return:
    """
    ...

def dumps(obj: object) -> str:
    """
    Return obj represented as a JSON string.
    :param obj:
    :return:
    """
    ...

def load(stream: str) -> dict:
    """
    Parse the given stream, interpreting it as a JSON string and deserialising the data to a Python object. The
    resulting object is returned.
    Parsing continues until end-of-file is encountered. A ValueError is raised if the data in stream is not correctly
    formed.
    :param stream:
    :return:
    """
    ...

def loads(str: str) -> dict:
    """
    Parse the JSON str and return an object. Raises ValueError if the string is not correctly formed.
    :param str:
    :return:
    """
    ...
