# A GraphDB Middleware for GoReplay
## Purpose
This application is designed to be used as GoReplay middleware when replaying prerecorded
GraphDB traffic.

It performs the following functions:
* Handles RDF4J compound transactions
* Handles user authentication

## Building
### Dependencies:
* JDK 17 or above
* Apache Maven
* JUnit 4
* Jackson 2.17.1

### Build procedure
```
mvn clean install
```

## Recording and replaying traffic

### Recording
When capturing traffic under Linux/macOS/*BSD, GoReplay must be run as **root**, to create a raw socket and set the 
network interface to promiscuous mode. Under Windows, elevation is required.

When capturing GraphDB traffic **-input-raw-track-response** is mandatory to латер replay compound transactions 
sucessfully.

If you want to inhibit logging of authentication secrets, please pass 
**-http-disallow-header Authorization**. But in this case, the middleware will not generate 
authentication tokens, as the username will not be available.

Please note that TLS traffic (HTTPS) can only be captured using the **-input-raw-protocol binary**
option and *will not produce usable results*

The middleware application has no function when recording to a file and will actually
hinder the recording process by blocking recording of any requests involving compound
transactions.

Here is an example of calling GoReplay to log all traffic to a file:
```
$ sudo goreplay --input-raw :7200 -input-raw-track-response --output-file ./graphdb.gor 
```

Other useful options are:
```
-input-raw-engine libpcap
   	Intercept traffic using libpcap (default), `raw_socket` or `pcap_file`
-input-raw-promisc
    enable promiscuous mode
-http-disallow-url value
    A regexp to match requests against. Filter get matched against full url with domain. 
    Anything else will be forwarded
```

### Replaying traffic
To replay traffic, you must specify both the **-output-http** and the **-middleware** 
options. The parameter to http-output is just a network address and port, for example, 
"staging.domain.com:7200" or "10.20.30.40:7200". 

When replaying traffic through this middleware the **--output-http-track-response** 
option is mandatory. If it is not specified, the tool can't replay compound transactions
and any request part of a compound transaction will be enqueued until a new transaction
ID is received. This will never happen, and the tool will eventually run out of memory.

Please note that the middleware application has no knowledge as to what parameters
you called GoReplay with and can't detect and alert you of these errors.

The parameter to middleware must be this application. We recommend that you create a simple
bash or CMD.EXE script to start java and the middleware application like so:
```
#!/bin/sh

java -Dautorization.secret=GraphDBRulezBigTime -jar path/to/GraphDBGoReplayMiddleware-1.0.jar
```
You can use the ***authorization.secret*** property to enable rewriting authentication tokens.
The value used should be the same as the value you set for ***graphdb.auth.token.secret***
in the graphdb properties file.

In order for the authentication to work correctly, the same users must be present on both the source and target.
The password set for the users is not important and will not be used.

Traffic may be replayed in three ways:
* From a previously captured log
* Directly while capturing
* To a remote GoReplay server

### Replaying previously captured log
Example:
```
goreplay --input-file "captured.gor|200%" --middleware ./middleware.sh \
    --output-http 192.168.11.12 --output-http-track-response
```
The example above will replay the traffic recorded in the file captured.gor at 200% the speed
(twice the load).

### Replaying traffic without recording it
In this mode, you can use the tool to achieve close to zero downtime upgrade, as well as instantly
revert in case of disaster. You can also maintain an offsite hit spare.

In this mode, the input is traffic capture, and output is an HTTP target.

Example:
```
sudo goreplay --input-raw {server_or_lb_address}:7200 --input-raw-track-response \
        --middleware graphdb-mw.sh --output-http {target_server_or_lb}:7200 \
        --output-http-track-response
```
**Important:** For this use case, you must make sure, that the tool will not capture its own
requests, as an endless loop will result. If you specify "--input-raw :7200" in the example 
above, the tool will have no means of telling its own requests to {target_server_or_lb} apart, because 
they are going to the same port.

### Replaying traffic to remote GoReplay server (via a tunnel)
The majority of production environments are heavily firewalled, and the target server's 
network may not be directly reachable. In such a case, you can still replay traffic, without 
first recording it, by using two GoReplay instances interconnected over a single TCP
connection (potentially running over an SSH tunnel).

On the capturing side of the tunnel:
```
sudo goreplay --input-raw {server_or_lb_address}:7200 --input-raw-track-response \
    --output-tcp {server_address and port}
```

On the replaying side of the tunnel:
```
goreplay --input-tcp :{port to listen to} --middleware ./middleware.sh --output-http-track-response \
    -output-http {target server address}:7200
```

If you want a secure tunnel, the following options may be useful to you:
```
  -input-tcp-certificate string
    	Path to PEM encoded certificate file. Used when TLS turned on.
  -input-tcp-certificate-key string
    	Path to PEM encoded certificate key file. Used when TLS turned on.
  -input-tcp-secure
    	Turn on TLS security. Do not forget to specify certificate and key files.
  -output-tcp-secure
    	Use TLS secure connection.
```
