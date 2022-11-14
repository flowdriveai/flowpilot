import os
import json

from common.params import Params, ParamKeyType

import urllib3

API_HOST = os.getenv('API_HOST', 'https://api.flowdrive.ai')

class Api():
  def __init__(self):
    self.params = Params()
    self.email = self.params.get("Email")
    self.token = self.params.get("Token")

    self.http_client = urllib3.PoolManager()

  def get_credentials(self):
    # Get STS
    r = self.http_client.request(
        'POST',
        f"{API_HOST}/auth/sts",
        fields={'email': self.email, 'token': self.token}
    )

    credentials = json.loads(r.data.decode('utf-8'))
    return credentials
