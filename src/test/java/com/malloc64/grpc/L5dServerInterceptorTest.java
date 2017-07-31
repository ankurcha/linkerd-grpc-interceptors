package com.malloc64.grpc;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.grpc.testing.integration.Messages;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.atomic.AtomicReference;

import static io.grpc.Metadata.*;
import static io.grpc.testing.integration.TestServiceGrpc.*;

@RunWith(JUnit4.class)
public class L5dServerInterceptorTest {
    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    private AtomicReference<Metadata> l5dMetadataFoundInContext = new AtomicReference<>(null);

    @Before
    public void setUp() throws Exception {
        TestServiceImplBase testServiceImpl = new TestServiceImplBase() {
            @Override
            public void unaryCall(Messages.SimpleRequest request,
                                  StreamObserver<Messages.SimpleResponse> responseObserver) {
                Context ctx = Context.current();
                l5dMetadataFoundInContext.set(Constants.L5D_CONTEXT_KEY.get(ctx));
                responseObserver.onNext(Messages.SimpleResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };

        l5dMetadataFoundInContext.set(null);
        grpcServerRule.getServiceRegistry()
                .addService(ServerInterceptors.intercept(testServiceImpl, new L5dServerInterceptor()));
    }

    @Test
    public void l5d_headers_are_put_in_current_context() throws Exception {
        ClientInterceptor addl5dHeadersInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                       CallOptions callOptions, Channel next) {
                ClientCall<ReqT, RespT> clientCall = next.newCall(method, callOptions);
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        // should be propagated
                        headers.put(Key.of("l5d-foo", ASCII_STRING_MARSHALLER), "foo-value");
                        headers.put(Key.of("l5d-bar", ASCII_STRING_MARSHALLER), "bar-value");
                        super.start(responseListener, headers);
                    }
                };
            }
        };
        TestServiceBlockingStub stub = newBlockingStub(grpcServerRule.getChannel())
                                                                      .withInterceptors(addl5dHeadersInterceptor);

        // do request
        stub.withInterceptors(addl5dHeadersInterceptor)
                                           .unaryCall(Messages.SimpleRequest.getDefaultInstance());

        // confirm that the value was present in the context
        Metadata ctxContents = l5dMetadataFoundInContext.get();
        Assert.assertNotNull(ctxContents);
        Assert.assertEquals("foo-value", ctxContents.get(Key.of("l5d-foo", ASCII_STRING_MARSHALLER)));
        Assert.assertEquals("bar-value", ctxContents.get(Key.of("l5d-bar", ASCII_STRING_MARSHALLER)));
    }
}