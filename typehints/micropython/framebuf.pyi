"""

Frame buffer manipulation

Descriptions taken from 
`https://raw.githubusercontent.com/micropython/micropython/master/docs/library/framebuf.rst`, etc.

This module provides a general frame buffer which can be used to create
bitmap images, which can then be sent to a display.

"""



__author__ = "Howard C Lovatt"
__copyright__ = "Howard C Lovatt, 2020 onwards."
__license__ = "MIT https://opensource.org/licenses/MIT (as used by MicroPython)."
__version__ = "4.0.0"  # Version set by https://github.com/hlovatt/tag2ver



from typing import TypeVar, overload, Final

from uarray import array


_AnyWritableBuf = TypeVar('_AnyWritableBuf', bytearray, array, memoryview)
"""
Type that allows bytearray, array, or memoryview, but only one of these and not a mixture in a single declaration.
"""




MONO_VLSB: Final[int] = ...
"""
Monochrome (1-bit) color format
    This defines a mapping where the bits in a byte are vertically mapped with
    bit 0 being nearest the top of the screen. Consequently each byte occupies
    8 vertical pixels. Subsequent bytes appear at successive horizontal
    locations until the rightmost edge is reached. Further bytes are rendered
    at locations starting at the leftmost edge, 8 pixels lower.
"""




MONO_HLSB: Final[int] = ...
"""
Monochrome (1-bit) color format
    This defines a mapping where the bits in a byte are horizontally mapped.
    Each byte occupies 8 horizontal pixels with bit 7 being the leftmost.
    Subsequent bytes appear at successive horizontal locations until the
    rightmost edge is reached. Further bytes are rendered on the next row, one
    pixel lower.
"""




MONO_HMSB: Final[int] = ...
"""
Monochrome (1-bit) color format
    This defines a mapping where the bits in a byte are horizontally mapped.
    Each byte occupies 8 horizontal pixels with bit 0 being the leftmost.
    Subsequent bytes appear at successive horizontal locations until the
    rightmost edge is reached. Further bytes are rendered on the next row, one
    pixel lower.
"""




RGB565: Final[int] = ...
"""
Red Green Blue (16-bit, 5+6+5) color format
"""




GS2_HMSB: Final[int] = ...
"""
Grayscale (2-bit) color format
"""




GS4_HMSB: Final[int] = ...
"""
Grayscale (4-bit) color format
"""




GS8: Final[int] = ...
"""
Grayscale (8-bit) color format
"""




class FrameBuffer:
   """

   
   The FrameBuffer class provides a pixel buffer which can be drawn upon with
   pixels, lines, rectangles, text and even other FrameBuffer's. It is useful
   when generating output for displays.
   
   For example::
   
       import framebuf
   
       # FrameBuffer needs 2 bytes for every RGB565 pixel
       fbuf = framebuf.FrameBuffer(bytearray(10 * 100 * 2), 10, 100, framebuf.RGB565)
   
       fbuf.fill(0)
       fbuf.text('MicroPython!', 0, 0, 0xffff)
       fbuf.hline(0, 10, 96, 0xffff)
   
   """



   @overload
   def __init__(self, buffer: _AnyWritableBuf, width: int, height: int, format: int, /):
      """
       Construct a FrameBuffer object.  The parameters are:
       
           - *buffer* is an object with a buffer protocol which must be large
             enough to contain every pixel defined by the width, height and
             format of the FrameBuffer.
           - *width* is the width of the FrameBuffer in pixels
           - *height* is the height of the FrameBuffer in pixels
           - *format* specifies the type of pixel used in the FrameBuffer;
             permissible values are listed under Constants below. These set the
             number of bits used to encode a color value and the layout of these
             bits in *buffer*.
             Where a color value c is passed to a method, c is a small integer
             with an encoding that is dependent on the format of the FrameBuffer.
           - *stride* is the number of pixels between each horizontal line
             of pixels in the FrameBuffer. This defaults to *width* but may
             need adjustments when implementing a FrameBuffer within another
             larger FrameBuffer or screen. The *buffer* size must accommodate
             an increased step size.
       
       One must specify valid *buffer*, *width*, *height*, *format* and
       optionally *stride*.  Invalid *buffer* size or dimensions may lead to
       unexpected errors.
      """

   @overload
   def __init__(self, buffer: _AnyWritableBuf, width: int, height: int, format: int, stride: int, /):
      """
       Construct a FrameBuffer object.  The parameters are:
       
           - *buffer* is an object with a buffer protocol which must be large
             enough to contain every pixel defined by the width, height and
             format of the FrameBuffer.
           - *width* is the width of the FrameBuffer in pixels
           - *height* is the height of the FrameBuffer in pixels
           - *format* specifies the type of pixel used in the FrameBuffer;
             permissible values are listed under Constants below. These set the
             number of bits used to encode a color value and the layout of these
             bits in *buffer*.
             Where a color value c is passed to a method, c is a small integer
             with an encoding that is dependent on the format of the FrameBuffer.
           - *stride* is the number of pixels between each horizontal line
             of pixels in the FrameBuffer. This defaults to *width* but may
             need adjustments when implementing a FrameBuffer within another
             larger FrameBuffer or screen. The *buffer* size must accommodate
             an increased step size.
       
       One must specify valid *buffer*, *width*, *height*, *format* and
       optionally *stride*.  Invalid *buffer* size or dimensions may lead to
       unexpected errors.
      """

   def fill(self, c: int, /) -> None:
      """
       Fill the entire FrameBuffer with the specified color.
      """

   @overload
   def pixel(self, x: int, y: int, /) -> int:
      """
       If *c* is not given, get the color value of the specified pixel.
       If *c* is given, set the specified pixel to the given color.
      """

   @overload
   def pixel(self, x: int, y: int, c: int, /) -> None:
      """
       If *c* is not given, get the color value of the specified pixel.
       If *c* is given, set the specified pixel to the given color.
      """

   def hline(self, x: int, y: int, w: int, c: int, /) -> None:
      """
       Draw a line from a set of coordinates using the given color and
       a thickness of 1 pixel. The `line` method draws the line up to
       a second set of coordinates whereas the `hline` and `vline`
       methods draw horizontal and vertical lines respectively up to
       a given length.
      """

   def vline(self, x: int, y: int, h: int, c: int, /) -> None:
      """
       Draw a line from a set of coordinates using the given color and
       a thickness of 1 pixel. The `line` method draws the line up to
       a second set of coordinates whereas the `hline` and `vline`
       methods draw horizontal and vertical lines respectively up to
       a given length.
      """

   def line(self, x1: int, y1: int, x2: int, y2: int, c: int, /) -> None:
      """
       Draw a line from a set of coordinates using the given color and
       a thickness of 1 pixel. The `line` method draws the line up to
       a second set of coordinates whereas the `hline` and `vline`
       methods draw horizontal and vertical lines respectively up to
       a given length.
      """

   def rect(self, x: int, y: int, w: int, h: int, c: int, /) -> None:
      """
       Draw a rectangle at the given location, size and color. The `rect`
       method draws only a 1 pixel outline whereas the `fill_rect` method
       draws both the outline and interior.
      """

   def fill_rect(self, x: int, y: int, w: int, h: int, c: int, /) -> None:
      """
       Draw a rectangle at the given location, size and color. The `rect`
       method draws only a 1 pixel outline whereas the `fill_rect` method
       draws both the outline and interior.
      """

   def text(self, s: str, x: int, y: int, c: int = 1, /) -> None:
      """
       Write text to the FrameBuffer using the the coordinates as the upper-left
       corner of the text. The color of the text can be defined by the optional
       argument but is otherwise a default value of 1. All characters have
       dimensions of 8x8 pixels and there is currently no way to change the font.
      """

   def scroll(self, xstep: int, ystep: int, /) -> None:
      """
       Shift the contents of the FrameBuffer by the given vector. This may
       leave a footprint of the previous colors in the FrameBuffer.
      """

   @overload
   def blit(self, fbuf: "FrameBuffer", x: int, y: int, /) -> None:
      """
       Draw another FrameBuffer on top of the current one at the given coordinates.
       If *key* is specified then it should be a color integer and the
       corresponding color will be considered transparent: all pixels with that
       color value will not be drawn.
       
       This method works between FrameBuffer instances utilising different formats,
       but the resulting colors may be unexpected due to the mismatch in color
       formats.
      """

   @overload
   def blit(self, fbuf: "FrameBuffer", x: int, y: int, key: int, /) -> None:
      """
       Draw another FrameBuffer on top of the current one at the given coordinates.
       If *key* is specified then it should be a color integer and the
       corresponding color will be considered transparent: all pixels with that
       color value will not be drawn.
       
       This method works between FrameBuffer instances utilising different formats,
       but the resulting colors may be unexpected due to the mismatch in color
       formats.
      """


