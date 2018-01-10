"""simple BTree database

The ``btree`` module implements a simple key-value database using external
storage (disk files, or in general case, a random-access `stream`). Keys are
stored sorted in the database, and besides efficient retrieval by a key
value, a database also supports efficient ordered range scans (retrieval
of values with the keys in a given range). On the application interface
side, BTree database work as close a possible to a way standard `dict`
type works, one notable difference is that both keys and values must
be `bytes` objects (so, if you want to store objects of other types, you
need to serialize them to `bytes` first).

The module is based on the well-known BerkelyDB library, version 1.xx.

Example::

    import btree

    # First, we need to open a stream which holds a database
    # This is usually a file, but can be in-memory database
    # using uio.BytesIO, a raw flash partition, etc.
    # Oftentimes, you want to create a database file if it doesn't
    # exist and open if it exists. Idiom below takes care of this.
    # DO NOT open database with "a+b" access mode.
    try:
        f = open("mydb", "r+b")
    except OSError:
        f = open("mydb", "w+b")

    # Now open a database itself
    db = btree.open(f)

    # The keys you add will be sorted internally in the database
    db[b"3"] = b"three"
    db[b"1"] = b"one"
    db[b"2"] = b"two"

    # Assume that any changes are cached in memory unless
    # explicitly flushed (or database closed). Flush database
    # at the end of each "transaction".
    db.flush()

    # Prints b'two'
    print(db[b"2"])

    # Iterate over sorted keys in the database, starting from b"2"
    # until the end of the database, returning only values.
    # Mind that arguments passed to values() method are *key* values.
    # Prints:
    #   b'two'
    #   b'three'
    for word in db.values(b"2"):
        print(word)

    del db[b"2"]

    # No longer true, prints False
    print(b"2" in db)

    # Prints:
    #  b"1"
    #  b"3"
    for key in db:
        print(key)

    db.close()

    # Don't forget to close the underlying stream!
    f.close()
"""

from typing import Any, Optional, Iterable, Tuple


class _BTree:
    def close(self) -> None:
        """Close the database. It's mandatory to close the database at the end of
        processing, as some unwritten data may be still in the cache. Note that
        this does not close underlying stream with which the database was opened,
        it should be closed separately (which is also mandatory to make sure that
        data flushed from buffer to the underlying storage).
        """
        ...

    def flush(self) -> None:
        """Flush any data in cache to the underlying stream."""
        ...

    def __getitem__(self, key: bytes) -> bytes:
        ...

    def get(self, key: bytes, default: Optional[bytes] = None) -> Optional[bytes]:
        ...

    def __setitem__(self, key: bytes, val: bytes) -> None:
        ...

    def __detitem__(self, key: bytes) -> None:
        ...

    def __contains__(self, key: bytes) -> bool:
        ...

    def __iter__(self) -> Iterable[bytes]:
        """A BTree object can be iterated over directly (similar to a dictionary)
        to get access to all keys in order.
        """
        ...

    def keys(self, start_key: bytes = None, end_key: bytes = None, flags: int = None) -> Iterable[bytes]:
        ...

    def values(self, start_key: bytes = None, end_key: bytes = None, flags: int = None) -> Iterable[bytes]:
        ...

    def items(self, start_key: bytes = None, end_key: bytes = None, flags: int = None) -> Iterable[Tuple[bytes, bytes]]:
        ...


def open(stream: Any, *, flags: int = 0, pagesize: int = 0, cachesize: int = 0,
         minkeypage: int = 0) -> _BTree:
    ...


INCL: int
DESC: int
