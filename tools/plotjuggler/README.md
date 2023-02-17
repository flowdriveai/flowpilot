## Installation

Once you've [set up the flowpilot environment](../README.md), this command will download PlotJuggler and install our plugins:

`cd tools/plotjuggler && ./juggle.py --install`

## Usage

```
$ ./juggle.py -h
usage: juggle.py [-h] [--demo] [--qlog] [--ci] [--can] [--stream] [--layout [LAYOUT]] [--install] [--dbc DBC]
                 [route_or_segment_name] [segment_count]

A helper to run PlotJuggler on openpilot routes

positional arguments:
  route_or_segment_name
                        The route or segment name to plot (cabana share URL accepted) (default: None)
  segment_count         The number of segments to plot (default: None)

optional arguments:
  -h, --help            show this help message and exit
  --qlog                Use qlogs (default: False)
  --can                 Parse CAN data (default: False)
  --stream              Start PlotJuggler in streaming mode (default: False)
  --layout [LAYOUT]     Run PlotJuggler with a pre-defined layout (default: None)
  --install             Install or update PlotJuggler + plugins (default: False)
  --dbc DBC             Set the DBC name to load for parsing CAN data. If not set, the DBC will be automatically
                        inferred from the logs. (default: None)

```

Examples using route name:

`./juggle.py "4cf7a6ad03080c90|2021-09-29--13-46-36"`

Examples using segment name:

`./juggle.py "4cf7a6ad03080c90|2021-09-29--13-46-36--1"`

## Layouts

If you create a layout that's useful for others, consider upstreaming it.

### Tuning

Use this layout to improve your car's tuning and generate plots for tuning PRs. Also see the [tuning wiki](https://github.com/commaai/openpilot/wiki/Tuning) and tuning PR template.

`--layout layouts/tuning.xml`


![screenshot](https://i.imgur.com/cizHCH3.png)
