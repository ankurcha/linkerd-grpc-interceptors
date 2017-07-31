package com.malloc64.grpc;

import io.grpc.*;
import io.grpc.testing.GrpcServerRule;
import io.grpc.testing.integration.Messages.SimpleResponse;
import io.grpc.testing.integration.TestServiceGrpc;
import io.grpc.testing.integration.TestServiceGrpc.TestServiceImplBase;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import static io.grpc.Metadata.*;
import static io.grpc.testing.integration.Messages.SimpleRequest;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class l5dClientInterceptorTest {
    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

    private final ServerInterceptor mockServerInterceptor = spy(new ServerInterceptor() {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                     ServerCallHandler<ReqT, RespT> next) {
            return next.startCall(call, headers);
        }
    });

    @Test
    public void client_adds_l5d_context_from_current_context() throws Exception {
        grpcServerRule.getServiceRegistry()
                .addService(ServerInterceptors.intercept(new TestServiceImplBase() { }, mockServerInterceptor));

        final TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(
                ClientInterceptors.intercept(grpcServerRule.getChannel(), new L5dClientInterceptor()));
        ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);

        // 1. lets construct a context with the l5d metadata populated
        Metadata l5d = new Metadata();
        l5d.put(Key.of("l5d-dtab", ASCII_STRING_MARSHALLER), "/host/web => /host/web-v2");
        l5d.put(Key.of("l5d-sample", ASCII_STRING_MARSHALLER), "0.98");
        Context ctxWithL5d = Context.current().withValue(Constants.L5D_CONTEXT_KEY, l5d);

        // 2. run rpc
        ctxWithL5d.run(new Runnable() {
            @Override
            public void run() {
                try {
                    stub.unaryCall(SimpleRequest.getDefaultInstance());
                    fail();
                } catch (StatusRuntimeException expected) {
                    // expected because the method is not implemented at server side
                }
            }
        });

        // 3. confirm that the contents of the l5d metadata form this context were received by the server
        verify(mockServerInterceptor).interceptCall(
                Matchers.<ServerCall<SimpleRequest, SimpleResponse>>any(),
                metadataCaptor.capture(),
                Matchers.<ServerCallHandler<SimpleRequest, SimpleResponse>>any());

        Metadata headers = metadataCaptor.getValue();
        Assert.assertEquals("/host/web => /host/web-v2", headers.get(Key.of("l5d-dtab", ASCII_STRING_MARSHALLER)));
        Assert.assertEquals("0.98", headers.get(Key.of("l5d-sample", ASCII_STRING_MARSHALLER)));
    }
}