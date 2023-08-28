xhost +
docker run -it --rm \
    -e DISPLAY:$DISPLAY \
    -v /tmp/.X11-unix:/tmp/.X11-unix:ro \
    --device=/dev/dri:/dev/dri \
    --mount type=volume,src=flowdrive,target=/root/.flowdrive \
    flowpilot