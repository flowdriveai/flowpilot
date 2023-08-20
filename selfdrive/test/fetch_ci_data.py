import os
import requests
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor

from selfdrive.test.openpilotci import get_url
from selfdrive.car.tests.routes import routes

CI_DATA_DIR = os.path.join(str(Path.home()), ".flowdrive", "ci-logs")

def download_file(url, destination):
    print(f"*starting download: {destination}")
    response = requests.get(url)
    if response.status_code == 200:
        os.makedirs(os.path.dirname(destination), exist_ok=True)
        with open(destination, 'wb') as f:
            f.write(response.content)
        print(f"downloaded {url} to {destination}")
    else:
        print(f"failed to download {destination}, status Code: {response.status_code}")

def download_files_threadpool(file_urls, destination_paths, max_workers=8):
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        executor.map(download_file, file_urls, destination_paths)

def main():
    file_urls = []
    destination_paths = []

    for route in routes:
        test_segs = (2, 1, 0)
        if route.segment is not None:
            test_segs = (route.segment,)
        for seg in test_segs:
            file_urls.append(get_url(route.route, seg))
            destination_paths.append(os.path.join(CI_DATA_DIR, f"{route.car_model}/{route.route}-{seg}/rlog.bz2"))
    download_file(file_urls[10], destination_paths[10])

if __name__ == "__main__":
    main()