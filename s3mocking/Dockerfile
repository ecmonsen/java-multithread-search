FROM scireum/s3-ninja

USER root
RUN apt install -y iproute2 sudo

RUN usermod -aG sudo sirius \
    && echo '%sudo ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers.d/nopasswd

# https://stackoverflow.com/questions/25497523/how-can-i-rate-limit-network-traffic-on-a-docker-container
# https://wiki.debian.org/TrafficControl
# https://github.com/moby/moby/issues/26767
COPY run2.sh /home/sirius/run2.sh
RUN chown sirius:sirius /home/sirius/run2.sh

USER sirius
CMD ["/bin/bash", "-c", "/home/sirius/run2.sh"]
