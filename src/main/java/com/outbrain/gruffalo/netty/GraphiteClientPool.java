package com.outbrain.gruffalo.netty;

import com.outbrain.gruffalo.util.Preconditions;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Time: 8/4/13 2:20 PM
 *
 * @author Eran Harel
 */
public class GraphiteClientPool implements GraphiteClient {

  private static final Logger logger = LoggerFactory.getLogger(GraphiteClientPool.class);
  private final GraphiteClient[] pool;
  private final AtomicInteger nextIndex = new AtomicInteger();

  public GraphiteClientPool(final EventLoopGroup eventLoopGroup,
                            final Throttler throttler,
                            final int inFlightBatchesHighThreshold,
                            final MetricFactory metricFactory,
                            final String graphiteRelayHosts) {
    Preconditions.checkNotNull(graphiteRelayHosts, "graphiteRelayHosts must not be null");
    Preconditions.checkNotNull(eventLoopGroup, "eventLoopGroup must not be null");

    logger.info("Creating a client pool for [{}]", graphiteRelayHosts);
    final String[] hosts = graphiteRelayHosts.trim().split(",");
    pool = new GraphiteClient[hosts.length];
    initClients(hosts, eventLoopGroup, throttler, inFlightBatchesHighThreshold, metricFactory);
  }

  private void initClients(final String[] hosts,
                           final EventLoopGroup eventLoopGroup,
                           final Throttler throttler,
                           final int inFlightBatchesHighThreshold,
                           final MetricFactory metricFactory) {
    for (int i = 0; i < hosts.length; i++) {
      final String[] hostAndPort = hosts[i].split(":");
      final String host = hostAndPort[0];
      final int port = Integer.parseInt(hostAndPort[1]);

      final NettyGraphiteClient client = new NettyGraphiteClient(throttler, inFlightBatchesHighThreshold, metricFactory, hosts[i]);
      pool[i] = client;
      final ChannelHandler graphiteChannelHandler = new GraphiteChannelInboundHandler(client, hosts[i], throttler);
      final GraphiteClientChannelInitializer channelInitializer = new GraphiteClientChannelInitializer(host, port, eventLoopGroup, graphiteChannelHandler);
      client.setChannelInitializer(channelInitializer);
    }
  }

  @Override
  public void connect() {
    for (final GraphiteClient client : pool) {
      client.connect();
    }
  }

  @Override
  public boolean publishMetrics(final String metrics) {
    final int currIndex = nextIndex.getAndIncrement();

    for(int i = 0; i < pool.length; i++) {
      final int currClientIndex = (i + currIndex) % pool.length;
      if (pool[currClientIndex].publishMetrics(metrics)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void onPushBack() {
    // meh
    // in the future we may implement some weighted round robin using this input
    // currently this is only used by the children to add metrics
  }
}
