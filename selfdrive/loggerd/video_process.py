import os
import subprocess
import json
from datetime import datetime
import numpy as np
import shutil
from selfdrive.loggerd.config import ROOT, VIDEO_LOGS, LOG_FORMAT, VIDEO_LOG_FORMAT, SEGMENT_LENGTH, VIDEO_EXTENSION


def make_chunks(vid_path, out_dir, prefix_name, chunk_duration=SEGMENT_LENGTH, skip=0, segment_start=0, ext=VIDEO_EXTENSION):
    os.makedirs(out_dir, exist_ok=True)
    subprocess.check_output(f"ffmpeg -ss {skip} -i {vid_path} -c copy -map 0 -segment_time {int(chunk_duration)} -segment_start_number {segment_start} -f segment {os.path.join(out_dir, f'{prefix_name}-%d.{ext}')}".split(" "),
                             stderr=subprocess.DEVNULL)

def closest_value(input_list, input_value):
    arr = np.asarray(input_list)
    return (np.abs(arr - input_value)).argmin()

def get_video_duration(filename):
    result = subprocess.check_output(
            f'ffprobe -v quiet -show_streams -select_streams v:0 -of json "{filename}"',
            shell=True).decode()
    fields = json.loads(result)['streams'][0]
    
    duration = fields['duration']
    return float(duration)

def segment_sync_videos():
    logs = os.listdir(ROOT)
    video_segments_dir = os.path.join(VIDEO_LOGS, ".segments")
    os.makedirs(video_segments_dir, exist_ok=True)

    video_names = os.listdir(VIDEO_LOGS)
    video_names.remove(".segments")

    segments_sofs = [datetime.strptime(log[:log.rfind("--")], LOG_FORMAT).timestamp() for log in logs]
    video_sofs = [datetime.strptime(video_name, VIDEO_LOG_FORMAT).timestamp() for video_name in video_names]

    # match video respective route
    idxs = []
    for vid_sof in video_sofs:
        idxs.append(closest_value(segments_sofs, vid_sof))

    for i, video_name in enumerate(video_names):
        print("processing video", video_name)
        full_video_path = os.path.join(VIDEO_LOGS, video_name)
        
        time_diff = video_sofs[i] - segments_sofs[idxs[i]]

        if abs(time_diff) > SEGMENT_LENGTH:
            continue

        if time_diff > 0:
            segment_start = 1
            skip = SEGMENT_LENGTH - time_diff
        else:
            segment_start = 0
            skip = abs(time_diff)
        
        log = logs[idxs[i]]
        route_name = log[:log.rfind("-")]

        current_video_segment_dir = os.path.join(video_segments_dir, route_name)
        os.makedirs(current_video_segment_dir, exist_ok=True)

        make_chunks(full_video_path, current_video_segment_dir, route_name, SEGMENT_LENGTH, skip, segment_start) 

        created_segments = os.listdir(current_video_segment_dir)
        for created_segment in created_segments:
            target_dir = os.path.join(ROOT, created_segment[:created_segment.rfind(".")])
            created_segment_path = os.path.join(current_video_segment_dir, created_segment)   
            
            # skip segments that dont have corresponding logs or are less than 1 second.
            if os.path.exists(target_dir) and get_video_duration(created_segment_path) > 1:
                shutil.move(created_segment_path, os.path.join(target_dir, f"fcam.{VIDEO_EXTENSION}"))
            else:
                os.remove(created_segment_path)
        os.rmdir(current_video_segment_dir)
        #os.remove(full_video_path)

if __name__ == "__main__":
    segment_sync_videos()