package com.malloc64.grpc;

import io.grpc.Context;
import io.grpc.Context.Key;
import io.grpc.Metadata;

final class Constants {
    static final String L5D_HEADER_PREFIX = "l5d-";
    static final Key<Metadata> L5D_CONTEXT_KEY = Context.key("l5d");
}
