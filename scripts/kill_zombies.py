import psutil

daemons = ["controlsd", "plannerd", "calibrationd", "pandad"
           "logmessaged", "keyvald", "radard"]
              
def kill():
    for proc in psutil.process_iter():
        if proc.name() in daemons:
            print(f"killing {proc.name()}..")
            proc.kill()
    print("done killing")
    
if __name__ == "__main__":
    kill()
