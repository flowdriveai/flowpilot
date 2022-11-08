import sentry_sdk
from sentry_sdk import capture_message

from selfdrive.version import is_dirty, get_commit, get_origin, get_short_branch
from selfdrive.swaglog import cloudlog
    
def sentry_init(prod=False) -> None:
    
    if not prod: 
        return

    env = get_short_branch()
    sentry_sdk.init(
        dsn="https://0e731cdaa07a4be3b53fe9f43d75a6ac@o4503930833338368.ingest.sentry.io/4503930835828736",
        ignore_errors=["KeyboardInterrupt"],
        traces_sample_rate=1.0,
    )

    sentry_sdk.set_tag("dirty", is_dirty())
    sentry_sdk.set_tag("origin", get_origin())
    sentry_sdk.set_tag("branch", get_short_branch())
    sentry_sdk.set_tag("commit", get_commit())

    sentry_sdk.Hub.current.start_session()

def capture_error(error, level) -> None:

    try:
        sentry_sdk.capture_message(error, level=level)
        sentry_sdk.flush()  # https://github.com/getsentry/sentry-python/issues/291
    except Exception:
        cloudlog.exception("sentry exception")