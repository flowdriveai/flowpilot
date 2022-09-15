import zmq  
import threading
from cereal.messaging.utils import get_zmq_socket_path
from common.params import Params


ctx = zmq.Context()
sock_get = ctx.socket(zmq.REP)
sock_get.bind(get_zmq_socket_path("6001")) # get socket
sock_put = ctx.socket(zmq.REP)
sock_put.bind(get_zmq_socket_path("6002")) # put socket
sock_del = ctx.socket(zmq.REP)
sock_del.bind(get_zmq_socket_path("6003")) # del socket

params = Params()

class ParamsServer:
    exit_event = threading.Event()
    threads = []

    @staticmethod
    def put_thread(exit_event):
        while not exit_event.is_set():
            key, val = sock_put.recv_multipart()
            params.put(key, val)
            sock_put.send(b"1")
        
    @staticmethod
    def get_thread(exit_event):
        while not exit_event.is_set():
            key = sock_get.recv()
            data = params.get(key)
            sock_get.send(data if data is not None else b"")

    @staticmethod
    def delete_thread(exit_event):
        while not exit_event.is_set():
            key = sock_del.recv()
            params.delete(key)
            sock_del.send(b"1")
    
    @staticmethod 
    def start():
        ParamsServer.exit_event.clear()
        if ParamsServer.threads:
            return
        ParamsServer.threads.append(threading.Thread(target=ParamsServer.put_thread, args=(ParamsServer.exit_event,), daemon=True))
        ParamsServer.threads.append(threading.Thread(target=ParamsServer.get_thread, args=(ParamsServer.exit_event,), daemon=True))
        ParamsServer.threads.append(threading.Thread(target=ParamsServer.delete_thread, args=(ParamsServer.exit_event,), daemon=True))
        
        for thread in ParamsServer.threads:
            thread.start()
            
    @staticmethod 
    def stop():
        ParamsServer.exit_event.set()
        ParamsServer.threads.clear()
    
    @staticmethod
    def wait():
        for thread in ParamsServer.threads:
            thread.join()
        
def main():
    try:
        ParamsServer.start()
        ParamsServer.wait()
    except KeyboardInterrupt:
        ParamsServer.stop()
          
if __name__ == "__main__":
    main()
