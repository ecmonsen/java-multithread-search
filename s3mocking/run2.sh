#!/bin/bash
set -euo pipefail
NETWORK_RATE="${NETWORK_RATE:-1024mbit}"
NETWORK_LATENCY="${NETWORK_LATENCY:-500ms}"
NETWORK_BURST="${NETWORK_BURST:-10k}"
sudo tc qdisc add dev eth0 root tbf rate $NETWORK_RATE latency $NETWORK_LATENCY burst $NETWORK_BURST
/bin/bash -c /home/sirius/run.sh
