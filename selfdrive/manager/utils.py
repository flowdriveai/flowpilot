import psutil
from cereal import log

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
