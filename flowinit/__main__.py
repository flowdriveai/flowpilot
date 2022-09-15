import logging

from flowinit import main, unset_init_lock

logger = logging.getLogger(__name__)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        logger.info("Received user interrupt. Cleaning files .. ")
        unset_init_lock()
