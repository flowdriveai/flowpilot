<img src="https://i.ibb.co/LZtKvfB/Screenshot-from-2022-09-15-22-15-14.png" alt="table" width="1270" />

# What is Flowpilot?

Flowpilot is an open source driver assistance system built on top of openpilot, that can run on most windows/linux and android powered machines. It performs the functions of Adaptive Cruise Control (ACC), Automated Lane Centering (ALC), Forward Collision Warning (FCW), Lane Departure Warning (LDW) and Driver Monitoring (DM) for a growing variety of supported car makes, models, and model years maintened by the community.

# Running on a Car

For running flowpilot on your car, you need: 

 - A supported machine to run flowpilot i.e. A windows, linux PC or android phone based on a snapdragon chip.
 - A white / grey panda with giraffe or a black panda with car harness. 
 - 1x USB-A to USB-A cable for connecting panda to PC and aditionally, an OTG cable is required if connecting panda to phone.
 - One of the [200+ supported cars](https://github.com/commaai/openpilot/blob/master/docs/CARS.md). The community supports Honda, Toyota, Hyundai, Nissan, Kia, Chrysler, Lexus, Acura, Audi, VW, and more. If your car is not supported but has adaptive cruise control and lane-keeping assist, it's likely able to run flowpilot.
 
# Installation:
See the [installation wiki](https://gitlab.com/selfdrive/flow-pilot/-/wikis/Installation).

# Running With a Virtual Car

It is recommended to develop on a virtual car / simulation before jumping onto testing on a real car. Flowpilot supports CARLA simulation. Optionally, you can use FlowStreamer to test flowpilot with any videogame. For more thorough testing, in addition to simulation, real panda hardware can be put in the loop for a more [thorough testing](https://twitter.com/flowdrive_ai/status/1566680576962478086).

# Community

Flowpilot's core community lives on the official flowdrive [discord server](https://discord.gg/mH8AUATafP) . Check the pinned messages or search history through messages to see if your issues or question has been discussed earlier. You may also join [other awesome](https://linktr.ee/flowdrive) openpilot discord communities. 

We also push frequent updates on our [twitter handle](https://twitter.com/flowdrive_ai).

# User Data 

Flowpilot will require your email address for setting up you flowdrive account. Flowpilot logs the road-facing cameras, CAN, GPS, IMU, magnetometer, thermal sensors, crashes, and operating system logs. The driver-facing camera is only logged if you explicitly opt-in in settings. The microphone is not recorded.

You understand that use of this software or its related services will generate certain types of user data, which may be logged and stored at the sole discretion of flowdrive. By accepting this agreement, you grant an irrevocable, perpetual, worldwide right to flowdrive for the use of this data.

# Disclaimer 

THIS IS ALPHA QUALITY SOFTWARE FOR RESEARCH PURPOSES ONLY. THIS IS NOT A PRODUCT. YOU ARE RESPONSIBLE FOR COMPLYING WITH LOCAL LAWS AND REGULATIONS. NO WARRANTY EXPRESSED OR IMPLIED.
