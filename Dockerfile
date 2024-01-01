FROM ubuntu:20.04

ENV PYTHONUNBUFFERED 1
ENV DEBIAN_FRONTEND="noninteractive"

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        sudo \
        ssh \
        tzdata \
        locales \
        git \
        nano \
        clang \
        libzmq3-dev \
        libjson11-1 \
        libjson11-1-dev \
        liblmdb-dev \
        libusb-1.0-0-dev \
        gcc-arm-none-eabi \
        libcurl4-openssl-dev \
        libssl-dev \
        ffmpeg \
        libeigen3-dev \
        software-properties-common \
        openjdk-11-jdk \
        python3.9 \
        python3.9-dev \
        python3.9-distutils \
        python3-pip \
        wget \
        unzip \
        make \
        libssl-dev \
        gcc \
        autoconf \
        automake \
        libtool \
        g++ \
        tmux \
        scons

RUN yes | sudo add-apt-repository ppa:ubuntu-toolchain-r/test
RUN apt upgrade libstdc++6 -y

RUN apt remove locales -y
RUN wget http://launchpadlibrarian.net/560614488/libc6_2.34-0ubuntu3_amd64.deb
RUN dpkg -i libc6_2.34-0ubuntu3_amd64.deb
RUN rm -rf libc6_2.34-0ubuntu3_amd64.deb

RUN mkdir -p /Android/Sdk/cmdline-tools/latest
RUN wget -q --show-progress --no-clobber --tries=5 https://dl.google.com/android/repository/commandlinetools-linux-8092744_latest.zip
RUN unzip commandlinetools-linux-8092744_latest.zip
RUN mv cmdline-tools/* /Android/Sdk/cmdline-tools/latest/
RUN rm -rf cmdline-tools commandlinetools-linux-8092744_latest.zip
RUN echo 'export ANDROID_SDK_ROOT=/Android/Sdk'
RUN echo 'export PATH=$PATH:/Android/Sdk/cmdline-tools/latest/bin'
RUN alias sdkmanager=/Android/Sdk/cmdline-tools/latest/bin/sdkmanager
RUN yes | /Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
RUN /Android/Sdk/cmdline-tools/latest/bin/sdkmanager --install "platform-tools"
RUN /Android/Sdk/cmdline-tools/latest/bin/sdkmanager --install "build-tools;32.0.0"
RUN yes | sudo add-apt-repository ppa:deadsnakes/ppa

WORKDIR /tmp
RUN git clone https://github.com/capnproto/capnproto.git
WORKDIR /tmp/capnproto  
RUN git checkout tags/v0.8.0
WORKDIR /tmp/capnproto/c++
RUN autoreconf -i
RUN ./configure
RUN make -j6 check
RUN sudo make install

RUN python3.9 -m pip install \
    pipenv \
    numpy \
    pycryptodome
RUN pipenv --python 3.9

COPY . /flowpilot

WORKDIR /flowpilot/libs/capnpc-java
RUN \
    if [ ! -d capnproto-java/ ]; \
    then \
        git clone https://github.com/capnproto/capnproto-java.git $DIR/capnproto-java; \
    fi
WORKDIR /flowpilot/libs/capnpc-java/capnproto-java
RUN git checkout 81d18463a8f3c98f6d21d4eae27caaca6bace4f7 
RUN make
RUN sudo make install

WORKDIR /flowpilot
RUN pipenv run pip install pkgconfig==1.5.5 Cython==0.29.32
RUN pipenv run pip install pycapnp==1.0.0
RUN pipenv run pip install -r requirements.txt
RUN touch .deb_update
RUN pipenv run scons
ENV ANDROID_SDK_ROOT=/Android/Sdk
RUN ./gradlew desktop:assemble