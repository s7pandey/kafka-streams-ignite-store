package com.shash.tracing.demo.ignite;

import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;

public class IgniteStoreSupplier implements KeyValueBytesStoreSupplier {

    private final String name;

    public IgniteStoreSupplier(final String name){
        this.name = name;
    }
    @Override
    public String name() {
        return name;
    }

    @Override
    public KeyValueStore<Bytes, byte[]> get() {
        return new IgniteStore(name);
    }

    @Override
    public String metricsScope() {
        return null;
    }
}
