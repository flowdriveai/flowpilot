import os
import logging
import json

from selfdrive.swaglog import cloudlog
from common.params import Params

import urllib3

logger = logging.getLogger(__name__)

API_HOST = os.getenv('API_HOST', 'https://api.flowdrive.ai')

class Api():
  def __init__(self):
    self.params = Params()
    self.http_client = urllib3.PoolManager()

  def get_credentials(self):
    # get userdata
    self.email = self.params.get("UserEmail")
    self.token = self.params.get("UserToken")

    if self.email is None or self.token is None:
      logger.error(f"Error retrieving Email and/or Token")
    
    logger.debug(f"Fetched email {self.email} and token ***")

    # Get STS
    r = self.http_client.request(
        'POST',
        f"{API_HOST}/auth/sts",
        fields={'email': self.email, 'token': self.token}
    )
    cloudlog.info("api init statuscode %d", r.status)

    credentials = json.loads(r.data.decode('utf-8'))
    return credentials
