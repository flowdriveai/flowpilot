import logging
import os
import sys
import tempfile
from pathlib import Path
import psutil

logger = logging.getLogger(__name__)


class FileLock:
    def __init__(self, filename=__name__) -> None:
        self.filename = filename

    def __enter__(self) -> None:
        """Sets the lock for running a single init process"""

        tempdir = tempfile.gettempdir()
        lockpath = os.path.join(tempdir, f"{self.filename}.lockfile")

        procs = [p.name for p in psutil.process_iter()]

        if os.path.isfile(lockpath) and ("flowinit" in procs):
            # An instance is already running
            sys.exit(
                "Cannot obtain a new lock."
                f"An instance of {self.filename} is already running."
            )
        else:
            # Run a new instance
            Path(lockpath).touch()
            logger.debug(f"{self.filename} lock set")

    def __exit__(self, exc_type, exc_value, tb) -> None:
        """Unsets the lock for letting a new process run"""

        tempdir = tempfile.gettempdir()
        lockpath = os.path.join(tempdir, f"{self.filename}.lockfile")

        logger.info("Cleaning files..")
        try:
            os.remove(lockpath)
            logger.debug(f"{self.filename} lock unset")
        except FileNotFoundError:
            logger.debug(f"{self.filename}: No such lock found")
