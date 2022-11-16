import sentry_sdk

from selfdrive.swaglog import cloudlog
from selfdrive.version import get_commit, get_origin, get_short_branch, is_dirty


def set_tag(key: str, value: str) -> None:
    sentry_sdk.set_tag(key, value)


def sentry_init(prod=False) -> None:
    
    if not prod: 
        return

    env = get_short_branch()
    sentry_sdk.init(
        dsn="https://0e731cdaa07a4be3b53fe9f43d75a6ac@o4503930833338368.ingest.sentry.io/4503930835828736",
        ignore_errors=["KeyboardInterrupt"],
        traces_sample_rate=1.0,
        environment=env,
    )

    set_tag("dirty", is_dirty())
    set_tag("origin", get_origin())
    set_tag("branch", get_short_branch())
    set_tag("commit", get_commit())

    sentry_sdk.Hub.current.start_session()


def report_tombstone(fn: str, message: str, contents: str) -> None:
  cloudlog.error({'tombstone': message})

  with sentry_sdk.configure_scope() as scope:
    scope.set_extra("tombstone_fn", fn)
    scope.set_extra("tombstone", contents)
    sentry_sdk.capture_message(message=message)
    sentry_sdk.flush()


def capture_error(error, level) -> None:

    try:
        sentry_sdk.capture_message(error, level=level)
        sentry_sdk.flush()  # https://github.com/getsentry/sentry-python/issues/291
    except Exception:
        cloudlog.exception("sentry exception")