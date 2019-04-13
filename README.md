# Routing Information Protocol Version 2
Submitted by Aneesh Joshi (aj4524@rit.edu)

This is an implementation of the Routing Information Protocol as submitted for Foundations of Computer Networks.

## Usage
- `java Rover [-h | --help]`
- `java Rover [-p | --port] 520 [-m | --multicastIp] 233.0.0.0  [-i | --id] 10`

### Example:
`java Rover --port 520 --multicastIp 233.0.0.0 --id 10`

## Note:
- I have provided a Dockerfile which I used for testing my implementation
- If you use port 520, you need to run it as sudo as ports below 1024 need root privilege
