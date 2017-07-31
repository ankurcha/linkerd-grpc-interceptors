package com.malloc64.grpc;

import io.grpc.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.malloc64.grpc.Constants.L5D_CONTEXT_KEY;

public class L5dClientInterceptor implements ClientInterceptor {
    private static final Logger log = Logger.getLogger(L5dServerInterceptor.class.getName());

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Context context = Context.current();
                Metadata l5d = L5D_CONTEXT_KEY.get(context);
                if (l5d == null) {
                    log.fine("No l5d context to propagate");
                } else {
                    log.log(Level.FINE, "Adding l5d context headers {0} to outbound request", new Object[]{l5d});
                    headers.merge(l5d);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
