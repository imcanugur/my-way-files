FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

ENV ANDROID_HOME=/usr/local/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Essential packages + libraries that AAPT2 might need during compilation
RUN rm -f /etc/apt/sources.list.d/yarn.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        wget unzip curl git libc6-i386 lib32z1 && \
    rm -rf /var/lib/apt/lists/*

# Android Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Confirm licenses and install SDK components.
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" \
               "platforms;android-34" "build-tools;34.0.0" \
               "platforms;android-35" "build-tools;35.0.0" \
               "platforms;android-36" "build-tools;36.0.0"

# Set permissions
RUN chmod -R a+rX ${ANDROID_HOME}

USER root