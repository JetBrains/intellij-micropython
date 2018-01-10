"""
Speech

Warning

WARNING! THIS IS ALPHA CODE.

We reserve the right to change this API as development continues.

The quality of the speech is not great, merely “good enough”.
Given the constraints of the device you may encounter memory errors
and / or unexpected extra sounds during playback. It’s early days
and we’re improving the code for the speech synthesiser all the time.
 Bug reports and pull requests are most welcome.

This module makes microbit talk, sing and make other
speech like sounds provided that you connect a speaker to your board.



Note

This work is based upon the amazing reverse engineering efforts of
Sebastian Macke based upon an old text-to-speech (TTS) program called
SAM (Software Automated Mouth) originally released in 1982 for the
Commodore 64. The result is a small C library that we have
adopted and adapted for the micro:bit. You can find out more from his homepage.
Much of the information in this document was gleaned from the
original user’s manual which can be found here.

The speech synthesiser can produce around 2.5 seconds
worth of sound from up to 255 characters of textual input.

To access this module you need to:

import speech

See http://microbit-micropython.readthedocs.io/en/latest/speech.html
for details
"""

def translate(words: str) ->None:
    """
    Given English words in the string words, return a string
    containing a best guess at the appropriate phonemes to pronounce.
    The output is generated from this text to phoneme translation table.

    This function should be used to generate a first approximation of phonemes
    that can be further hand-edited to improve accuracy, inflection and emphasis.
    """

def pronounce(phonemes: str, pitch: int=64, speed: int=72,
              mouth: int=128, throat: int=128)->None:
    """
    Pronounce the phonemes in the string phonemes. See below for details of how
    to use phonemes to finely control the output of the speech synthesiser.
    Override the optional pitch, speed, mouth and throat settings to change
    the timbre (quality) of the voice.
    """

def say(words: str, pitch: int=64, speed: int=72,
              mouth: int=128, throat: int=128)->None:
    """
    Say the English words in the string words. The result is semi-accurate
    for English.
    Override the optional pitch, speed, mouth and throat settings to change
    the timbre (quality) of the voice. This is a short-hand equivalent of:
    speech.pronounce(speech.translate(words))
    """

def sing(phonemes: str, pitch: int=64, speed: int=72,
              mouth: int=128, throat: int=128)->None:
    """
    Sing the phonemes contained in the string phonemes. Changing the pitch
    and duration of the note is described below. Override the optional pitch,
    speed, mouth and throat settings to change the timbre (quality) of the voice.
    """