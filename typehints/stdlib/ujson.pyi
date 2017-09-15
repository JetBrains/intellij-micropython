"""
This modules allows to convert between Python objects and the JSON
data format.
"""


def dumps(obj: object) -> str:
  """
  Return `obj` represented as a JSON string.

  :param obj: Object to dump
  :type obj: object
  :return: JSON string
  :rtype: str
  """
  ...


def loads(str: str) -> Any:
  """
  Parse the JSON `str` and return a value.  Raises ValueError if the
  string is not correctly formed.

  :param str: JSON string
  :type str: str
  :return: Value parsed from JSON string
  :rtype: Any
  """
  ...
