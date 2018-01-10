"""Frame buffer manipulation

This module provides a general frame buffer which can be used to create
bitmap images, which can then be sent to a display.
"""
from typing import Any, Optional, overload


class FrameBuffer:
    """The FrameBuffer class provides a pixel buffer which can be drawn upon with
    pixels, lines, rectangles, text and even other FrameBuffer's. It is useful
    when generating output for displays.

    For example::

        import framebuf

        # FrameBuffer needs 2 bytes for every RGB565 pixel
        fbuf = FrameBuffer(bytearray(10 * 100 * 2), 10, 100, framebuf.RGB565)

        fbuf.fill(0)
        fbuf.text('MicroPython!', 0, 0, 0xffff)
        fbuf.hline(0, 10, 96, 0xffff)
    """

    def __init__(self, buffer: Any, width: int, height: int, format: int,
                 stride: int = 0) -> None:
        """Construct a FrameBuffer object.  The parameters are:

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
        ...

    def fill(self, c: int) -> None:
        """Fill the entire FrameBuffer with the specified color."""
        ...

    @overload
    def pixel(self, x: int, y: int) -> int:
        """Get the color value of the specified pixel."""
        ...

    @overload
    def pixel(self, x: int, y: int, c: int) -> None:
        """Set the specified pixel to the given color."""
        ...

    def hline(self, x: int, y: int, w: int, c: int) -> None:
        """Draw a horizontal line from a set of coordinates using the given color and
        a thickness of 1 pixel."""
        ...

    def vline(self, x: int, y: int, h: int, c: int) -> None:
        """Draw a vertical line from a set of coordinates using the given color and
        a thickness of 1 pixel."""
        ...

    def line(self, x1: int, y1: int, x2: int, y2: int, c: int) -> None:
        """Draw a line from a set of coordinates using the given color and
        a thickness of 1 pixel."""
        ...

    def rect(self, x: int, y: int, w: int, h: int, c: int) -> None:
        """Draw a rectangle at the given location, size and color."""
        ...

    def fill_rect(self, x: int, y: int, w: int, h: int, c: int) -> None:
        """Fill a rectangle at the given location, size and color."""
        ...

    def text(self, s: str, x: int, y: int, c: int = 1) -> None:
        """Write text to the FrameBuffer using the the coordinates as the upper-left
        corner of the text. The color of the text can be defined by the optional
        argument but is otherwise a default value of 1. All characters have
        dimensions of 8x8 pixels and there is currently no way to change the font.
        """
        ...

    def scroll(self, xstep: int , ystep: int) -> None:
        """Shift the contents of the FrameBuffer by the given vector. This may
        leave a footprint of the previous colors in the FrameBuffer.
        """
        ...

    def blit(self, fbuf: FrameBuffer, x: int, y: int, key: Optional[int] = None) -> None:
        """Draw another FrameBuffer on top of the current one at the given coordinates.

        If *key* is specified then it should be a color integer and the
        corresponding color will be considered transparent: all pixels with that
        color value will not be drawn.

        This method works between FrameBuffer instances utilising different formats,
        but the resulting colors may be unexpected due to the mismatch in color
        formats.
        """
        ...


MONO_VLSB: int
MONO_HLSB: int
MONO_HMSB: int
RGB565: int
GS2_HMSB: int
GS4_HMSB: int
GS8: int
