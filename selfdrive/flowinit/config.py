import os


class Config:
    FREQUENCY = 1/2  # 2 Hz
    DAEMON_HOST = "houston"
    DAEMON_POLL_FREQ = 200  # in ms
    LOGPATH = os.path.join(
        os.path.dirname(os.path.realpath(__file__)), "logfiles"
    )
    FLOWINIT_ROOT = os.path.join(
        os.path.dirname(os.path.realpath(__file__)), "."
    )
    FLOWPILOT_ROOT = os.path.join(
        os.path.dirname(os.path.realpath(__file__)), "../"
    )
