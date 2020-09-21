"""
efficient arrays of numeric data

Descriptions taken from 
`https://raw.githubusercontent.com/micropython/micropython/master/docs/library/uarray.rst`, etc.
"""

__author__      = "Howard C Lovatt"
__copyright__   = "Howard C Lovatt, 2020 onwards."
__license__     = "MIT https://opensource.org/licenses/MIT (as used by MicroPython)."
__version__     = "0.0.0"



from typing import overload, Sequence, Any


class array:
   """
   |see_cpython_module| :mod:`python:array`.
   
   Supported format codes: ``b``, ``B``, ``h``, ``H``, ``i``, ``I``, ``l``,
   ``L``, ``q``, ``Q``, ``f``, ``d`` (the latter 2 depending on the
   floating-point support).
   """



   
   @overload
   def __init__(self, typecode: str, /): ...
   @overload
   def __init__(self, typecode: str, iterable: Sequence[Any], /):
      """
       Create array with elements of given type. Initial contents of the
       array are given by *iterable*. If it is not provided, an empty
       array is created.
      """


   def append(self, val: Any, /) -> None:
      """
           Append new element *val* to the end of array, growing it.
      """


   def extend(self, iterable: Sequence[Any], /) -> None:
      """
           Append new elements as contained in *iterable* to the end of
           array, growing it.
      """
