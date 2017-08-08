package com.malloc64.grpc;

import io.grpc.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.malloc64.grpc.Constants.L5D_CONTEXT_KEY;
import static com.malloc64.grpc.Constants.L5D_HEADER_PREFIX;

public class L5dServerInterceptor implements ServerInterceptor {
    private static final Logger log = Logger.getLogger(L5dServerInterceptor.class.getName());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        final Metadata l5d = new Metadata();
        for (String name : headers.keys()) {
            if (!name.toLowerCase().startsWith(L5D_HEADER_PREFIX)) {
                continue;
            }
            Metadata.Key<String> key = Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
            Iterable<String> values = headers.getAll(key);
            if (values == null) {
                continue;
            }
            for (String val : values) {
                l5d.put(key, val);
            }
        }
        log.log(Level.FINE, "Got headers: {0} propagating: {1}", new Object[]{headers, l5d});
        final ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ServerCall.Listener<ReqT>() {
            @Override
            public void onMessage(final ReqT message) {
                Context.current().withValue(L5D_CONTEXT_KEY, l5d).run(new Runnable() {
                    @Override
                    public void run() {
                        listener.onMessage(message);
                    }
                });
            }

            public void onHalfClose() {
                Context.current().withValue(L5D_CONTEXT_KEY, l5d).run(new Runnable() {
                    @Override
                    public void run() {
                        listener.onHalfClose();
                    }
                });
            }

            public void onCancel() {
                Context.current().withValue(L5D_CONTEXT_KEY, l5d).run(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCancel();
                    }
                });
            }

            public void onComplete() {
                Context.current().withValue(L5D_CONTEXT_KEY, l5d).run(new Runnable() {
                    @Override
                    public void run() {
                        listener.onComplete();
                    }
                });
            }

            public void onReady() {
                Context.current().withValue(L5D_CONTEXT_KEY, l5d).run(new Runnable() {
                    @Override
                    public void run() {
                        listener.onReady();
                    }
                });
            }
        };
    }
}
