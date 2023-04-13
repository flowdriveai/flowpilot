import logging
import tempfile
import zmq
import platform
from cereal.messaging.utils import get_zmq_socket_path

logger = logging.getLogger(__name__)
SHM_DIR = tempfile.gettempdir() + "/" if platform.system() == "Windows" else "@" # use abstract sockets on linux

class DaemonSig:
    START = b"start_flag"
    STOP = b"stop_flag"


class Daemon:
    """FlowInit Daemon"""

    def __init__(self):
        """Initialises the FlowInit Daemon socket and poller"""

        self.zmq_ctx = zmq.Context()
        self.socket = self.zmq_ctx.socket(zmq.PULL)

        # Bind to a IPC socket
        host = get_zmq_socket_path("6004")
        self.socket.bind(host)
        logger.debug(f"Connected daemon to {host}")

        # Define poller
        self.poller = zmq.Poller()
        self.poller.register(self.socket, zmq.POLLIN)

    def recv(self):
        """Polls and returns a byte message if it recieved one"""

        # Poll for messages
        poll_e = self.poller.poll(200)

        if len(poll_e) > 0:
            # Got a poll event

            sock, event = poll_e[0]
            if sock and event == zmq.POLLIN:
                # Got message, return it

                msg = sock.recv()
                logger.debug(f"recieved message: {msg}")
                return msg

        # Got nothing
        return None
