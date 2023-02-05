import psutil

from cereal import log
import cereal.messaging as messaging
from system.swaglog import cloudlog
from common.system import is_android, is_android_rooted


def get_device_state_msg():
    # System utilization
    msg = messaging.new_message("deviceState")
    du = psutil.disk_usage('/')
    
    msg.deviceState.freeSpacePercent = du.free / du.total * 100
    msg.deviceState.memoryUsagePercent = int(round(psutil.virtual_memory().percent))
    msg.deviceState.cpuUsagePercent = [int(round(n)) for n in psutil.cpu_percent(percpu=True)]

    # Power
    if is_android() and is_android_rooted():
        battery = psutil.sensors_battery()
        msg.deviceState.batteryPercent = int(battery.percent)
        msg.deviceState.chargingDisabled = battery.power_plugged

    # Device Thermals
    temps = psutil.sensors_temperatures()
    if temps.get("coretemp", None) is not None:
        msg.deviceState.cpuTempC = [cpu[1] for cpu in psutil.sensors_temperatures()['coretemp']][1:]

    fans = psutil.sensors_fans()
    if fans:
        msg.deviceState.fanSpeedPercentDesired = list(psutil.sensors_fans().values())[0][0][1]

    msg.deviceState.networkType = log.DeviceState.NetworkType.none
    msg.deviceState.networkStrength = log.DeviceState.NetworkStrength.unknown

    return msg

def get_cpu_times():
    """system-wide vitals on CPU"""
    ret = []
    cpu_time_msg = log.ProcLog.CPUTimes.new_message()

    cpu_times = psutil.cpu_times(percpu=True)

    for i in range(len(cpu_times)):
        cpu_time_msg.cpuNum = i
        cpu_time_msg.user = cpu_times[i].user
        cpu_time_msg.system = cpu_times[i].system
        cpu_time_msg.idle = cpu_times[i].idle
        cpu_time_msg.nice = cpu_times[i].nice
        cpu_time_msg.iowait = cpu_times[i].iowait
        cpu_time_msg.irq = cpu_times[i].irq
        cpu_time_msg.softirq = cpu_times[i].softirq

        ret.append(cpu_time_msg)
    return ret
        


def get_memory_logs():
    """system-wide vitals on Memory"""

    mem_msg = log.ProcLog.Mem.new_message()
    svmem = psutil.virtual_memory()

    mem_msg.total = svmem.total
    mem_msg.free = svmem.free
    mem_msg.available = svmem.available
    mem_msg.buffers = svmem.buffers
    mem_msg.cached = svmem.cached
    mem_msg.active = svmem.active
    mem_msg.inactive = svmem.inactive
    mem_msg.shared = svmem.shared

    return mem_msg
