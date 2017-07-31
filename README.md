# linkerd-grpc-interceptors

[ ![Download](https://api.bintray.com/packages/ankurcha/maven/linkerd-grpc-interceptors/images/download.svg) ](https://bintray.com/ankurcha/maven/linkerd-grpc-interceptors/_latestVersion)

Special thanks to: 

*Spencer Fang* - https://groups.google.com/forum/#!topic/grpc-io/Eke0FzdY9go


## Usage

In order to correctly forward headers from linkerd proxy. Add the `L5dServerInterceptor` instance to the using the 
`ServerInterceptors` utility class. For example:

```java
...
GrpcServiceImpl serviceImpl = ...
ServerInterceptors.intercept(serviceImpl, new L5dServerInterceptor());
...
```

Similarly, while creating a new client add the `L5dClientInterceptor` instance to the channel using the 
`ClientInterceptors` utility class. For example:

```java
...
GrpcService.newBlockingStub(ClientInterceptors.intercept(nettyChannel, new L5dClientInterceptor()));
...
```

## Links

* [Linkerd Header Reference](https://linkerd.io/config/head/linkerd/index.html#http-2-headers)
* [Grpc java](https://github.com/grpc/grpc-java)
* [Original grpc-io mailing list discussion](https://groups.google.com/forum/#!topic/grpc-io/Eke0FzdY9go)
