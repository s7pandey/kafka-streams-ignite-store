package com.shash.tracing.demo.kafka;

import com.shash.tracing.demo.config.PropertiesConfig;
import com.shash.tracing.demo.ignite.IgniteStoreSupplier;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;

import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.stereotype.Component;

@Component
@EnableKafkaStreams
public class BuildStreams {

  @Bean
  public StreamsBuilder buildAllStreams(
      StreamsBuilder streamsBuilder,
      PropertiesConfig propertiesConfig,
      Materialized<String, String, KeyValueStore<Bytes, byte[]>> reduceStore) {
    streamsBuilder.stream(
            propertiesConfig.getInputTopic(), Consumed.with(Serdes.String(), Serdes.String()))
        //        .map((key, value) -> KeyValue.pair(key, value))
        .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
        .reduce((currentVal, aggVal) -> aggVal, reduceStore);
    return streamsBuilder;
  }
}
