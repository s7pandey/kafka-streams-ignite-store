package com.shash.tracing.demo.kafka;

import com.shash.tracing.demo.ignite.IgniteStoreSupplier;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Stores {

  @Bean
  public Materialized<String, String, KeyValueStore<Bytes, byte[]>> reduceStateStore() {
    return Materialized.<String, String>as(new IgniteStoreSupplier("reduce-store"))
        .withKeySerde(Serdes.String())
        .withValueSerde(Serdes.String())
        .withCachingDisabled();
  }
}
