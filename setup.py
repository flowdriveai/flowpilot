from setuptools import setup, find_packages
import subprocess


def install_local(dep_path):
    subprocess.check_call(["pip", "install", "-e", dep_path])
    
def get_requirements():
   subprocess.check_call(["pip", "install", "-r", "requirements.txt"])

setup(name="flowpilot",
      version="0.1.0",
      packages=find_packages(),
      py_modules=["controlsd", "plannerd", "calibrationd", "logmessaged", "flowinit"],
      entry_points={"console_scripts": ["controlsd=selfdrive.controls.controlsd:main",
                                        "plannerd=selfdrive.controls.plannerd:main",
                                        "radard=selfdrive.controls.radard:main",
                                        "calibrationd=selfdrive.calibration.calibrationd:main",
                                        "logmessaged=selfdrive.logmessaged:main",
                                        "keyvald=selfdrive.keyvald:main",
                                        "pandad=selfdrive.boardd.pandad:run",
                                        "uploader=selfdrive.loggerd.uploader:main",
                                        "deleter=selfdrive.loggerd.deleter:main",
                                        "statsd=selfdrive.statsd:main",
                                        "thermald_=selfdrive.thermald.thermald:main", # thermald name is reserverd
                                        "flowinit=selfdrive.manager.flowinitd:main"]}
     )

