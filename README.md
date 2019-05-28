# Routing Information Protocol Version 2
This project involved 2 parts:
- The Routing Information Protocol which allows Rovers/Routers to discover each other over a network (simulated with multicast) and decide the best path for data transfer
- Reliable Data Transfer over UDP (using my own design), described [here](https://github.com/aneesh-joshi/routing-information-protocol-v2/blob/master/JRTP%20Specification.pdf)

## Usage
- `java Rover [-h | --help]`
- `java Rover [-p | --port] 520 [-m | --multicastIp] 233.0.0.0  [-i | --id] 10`

### Example:
`java Rover --port 520 --multicastIp 233.0.0.0 --id 10`

## Note:
- I have provided a Dockerfile which I used for testing my implementation
- If you use port 520, you need to run it as sudo as ports below 1024 need root privilege
