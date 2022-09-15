import tkinter
from tkinter import ttk
from tkinter import messagebox, StringVar
import time
import os
import time
import sys, getopt
import numpy as np
import threading
import cv2
import win32gui, win32ui, win32con, win32api
import ctypes
from guiConfig import Config
from threadUtils import synchronized
from windowStreamer import WindowStreamer


class Player:
    def __init__(self, window, window_title, streamer, columns=Config.WIDTH):
        self.window = window
        self.window.resizable(0, 0)
        self.window.title(window_title)
        self.streamer = streamer

        self.search_label = tkinter.Label(window, text="Window Name:", bg=Config.BG_COLOR, fg=Config.TEXT_COLOR, padx=10)
        self.search_label.grid(row=0, column=0)

        self.search_bar = ttk.Combobox(window, width=columns)
        self.search_bar.grid(row=0, column=1, columnspan=columns)

        self.search_button_icon = tkinter.PhotoImage(file=self.getResource("search_button.png"))
        self.search_button = tkinter.Button(window, image=self.search_button_icon, command=self.updateSearch, bg=Config.BG_COLOR, activebackground=Config.ACTIVE_COLOR)
        self.search_button.grid(row=0, column=columns+1)

        self.topic_label = tkinter.Label(window, text="Topic:", bg=Config.BG_COLOR, fg=Config.TEXT_COLOR, padx=5)
        self.topic_label.grid(row=1, column=0)

        self.topic_entry = tkinter.Entry(window, textvariable=StringVar(window, 'roadCameraState'), width=columns//6)
        self.topic_entry.grid(row=1, column=1)

        self.stream_label = tkinter.Label(window, text="Stream:", bg=Config.BG_COLOR, fg=Config.TEXT_COLOR, padx=10, font=("Ariel", 10))
        self.stream_label.grid(row=1, column=columns//2-11)

        self.btn_stream=tkinter.Button(window, command=self.stream_toggle, bg=Config.BG_COLOR, activebackground=Config.ACTIVE_COLOR)
        self.btn_stream.grid(row=1, column=columns//2-10)

        self.play_button_icon = tkinter.PhotoImage(file=self.getResource("play_button.png"))
        self.btn_play=tkinter.Button(window, image=self.play_button_icon, command=self.play, bg=Config.BG_COLOR, activebackground=Config.ACTIVE_COLOR)
        self.btn_play.grid(row=1, column=columns//2 - 9)

        self.pause_button_icon = tkinter.PhotoImage(file=self.getResource("pause_button.png"))
        self.btn_pause=tkinter.Button(window, image=self.pause_button_icon, command=self.pause, bg=Config.BG_COLOR, activebackground=Config.ACTIVE_COLOR)
        self.btn_pause.grid(row=1, column=columns//2-8)

        self.img_on = tkinter.PhotoImage(file = self.getResource("on.png"))
        self.img_off = tkinter.PhotoImage(file = self.getResource("off.png"))
        self.btn_stream.config(image=self.img_off)

        # After update is called once, the update method will be automatically called every delay milliseconds
        self.update()
        self.disablePlayButtons()
        self.window.configure(bg=Config.BG_COLOR)
        self.window.iconbitmap(self.getResource("flow.ico"))
        self.window.mainloop()

    def updateSearch(self):
        self.streamer.updateAvailableWindows()
        query = self.search_bar.get()
        search_res = self.streamer.searchWindow(query)
        self.search_bar['values'] = list(search_res.keys())
        self.search_bar.event_generate('<Down>')

    def pause(self):
        self.streamer.pause()
        self.enablePlayButtons()
        self.btn_pause["state"] = "disabled"

    def play(self):
        self.streamer.resume()
        self.enablePlayButtons()
        self.btn_play["state"] = "disabled"

    def disablePlayButtons(self):
        self.btn_play["state"] = "disabled"
        self.btn_pause["state"] = "disabled"

    def enablePlayButtons(self):
        self.btn_play["state"] = "normal"
        self.btn_pause["state"] = "normal"

    def stopStream(self):
        self.btn_stream.config(image=self.img_off)
        self.streamer.stopStreaming()
        self.disablePlayButtons()

    def startStream(self):
        query = self.search_bar.get()
        topic = self.topic_entry.get()
        ret = self.streamer.startStreaming(query, topic)
        if not ret:
            messagebox.showerror("Error", "Window '%s' not found. Are you sure the window name is correct ? Try using the search button."%query)
            return
        self.btn_stream.config(image=self.img_on)
        self.enablePlayButtons()
        self.btn_play["state"] = "disabled"

    def stream_toggle(self):
        if self.streamer.isStreaming():
            self.stopStream()
        else:
            self.startStream()

    def update(self):
        self.window.after(25, self.update)
        self.pushErrors(self.streamer.getErrors())

    def pushErrors(self, errs):
        if errs: self.stopStream()
        for err in errs:
            messagebox.showerror("Error", err)

    def getResource(self, name):
        return os.path.join(Config.RESORCES, name)

    def dispose(self):
        self.streamer.stopStreaming()

def main():
    streamer = WindowStreamer((874, 1164))
    player = Player(tkinter.Tk(), "FlowStreamer", streamer)
    player.dispose()

if __name__ == "__main__":
    main()
