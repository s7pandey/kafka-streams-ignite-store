package com.shash.tracing.demo.ignite;

import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.errors.ProcessorStateException;
import org.apache.kafka.streams.processor.BatchingStateRestoreCallback;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.*;

public class IgniteStore implements KeyValueStore<Bytes, byte[]> {

  private ClientCache<byte[], byte[]> cache;
  private IgniteClient client;
  private boolean isOpen = false;
  private final String name;

  public IgniteStore(final String name) {
    this.name = name;
  }

  @Override
  public void put(Bytes key, byte[] value) {
    Objects.requireNonNull(key, "key cannot be null");
    try {
      cache.putAsync(key.get(), value);
    } catch (ClientException e) {
      throw new ProcessorStateException("Could not put into cache", e);
    }
  }

  @Override
  public byte[] putIfAbsent(Bytes key, byte[] value) {
    Objects.requireNonNull(key, "key cannot be null");
    final byte[] originalVal = get(key);
    if (originalVal == null) {
      put(key, value);
    }
    return originalVal;
  }

  @Override
  public void putAll(List<KeyValue<Bytes, byte[]>> entries) {}

  public void putAllRestore(List<KeyValue<byte[], byte[]>> entries) {
    Map<byte[], byte[]> map = new HashMap<>();
    entries.forEach(keyValue -> map.put(keyValue.key, keyValue.value));
    cache.putAll(map);
  }

  @Override
  public byte[] delete(Bytes key) {
    Objects.requireNonNull(key, "key cannot be null");
    final byte[] originalVal = get(key);
    try {
      cache.remove(key.get());
    } catch (ClientException e) {
      throw new ProcessorStateException("Could not delete from cache", e);
    }
    return originalVal;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void init(ProcessorContext context, StateStore root) {
    String addresses = (String) context.appConfigs().get("apache.ignite.addresses");
    ClientConfiguration config =
        new ClientConfiguration().setAddresses(addresses == null ? "127.0.0.1:10800" : addresses);
    ClientCacheConfiguration cacheConfiguration =
        new ClientCacheConfiguration()
            .setName("state")
            .setCacheMode(CacheMode.LOCAL)
            .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);

    try {
      client = Ignition.startClient(config);
      cache = client.getOrCreateCache(cacheConfiguration);
    } catch (Exception e) {
      throw new ProcessorStateException("Error connecting to Ignite", e);
    }
    isOpen = true;
    context.register(root, new IgniteRestoreCallback(this));
  }

  @Override
  public void flush() {}

  @Override
  public void close() {
    try {
      client.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean persistent() {
    return false;
  }

  @Override
  public boolean isOpen() {
    return isOpen;
  }

  @Override
  public byte[] get(Bytes key) {
    try {
      return cache.get(key.get());
    } catch (ClientException e) {
      throw new ProcessorStateException("Could not get value", e);
    }
  }

  @Override
  public KeyValueIterator<Bytes, byte[]> range(Bytes from, Bytes to) {
    return null;
  }

  @Override
  public KeyValueIterator<Bytes, byte[]> all() {
    return null;
  }

  @Override
  public long approximateNumEntries() {
    return cache.size(CachePeekMode.ALL);
  }

  static class IgniteRestoreCallback implements BatchingStateRestoreCallback {

    private final IgniteStore igniteStore;

    public IgniteRestoreCallback(final IgniteStore igniteStore) {
      this.igniteStore = igniteStore;
    }

    @Override
    public void restoreAll(Collection<KeyValue<byte[], byte[]>> records) {
      igniteStore.putAllRestore(new ArrayList<>(records));
    }
  }
}
