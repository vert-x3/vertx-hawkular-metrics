package io.vertx.ext.metric.reporters.influxdb.impl;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.metric.collect.impl.AbstractSender;
import io.vertx.ext.metric.collect.impl.AvailabilityPoint;
import io.vertx.ext.metric.collect.impl.CounterPoint;
import io.vertx.ext.metric.collect.impl.DataPoint;
import io.vertx.ext.metric.collect.impl.GaugePoint;
import io.vertx.ext.metric.reporters.influxdb.AuthenticationOptions;
import io.vertx.ext.metric.reporters.influxdb.VertxInfluxDbOptions;
import io.vertx.ext.metric.reporters.influxdb.impl.InfluxDbSender;
import io.vertx.ext.metric.reporters.influxdb.impl.Point.Builder;

public class InfluxDbSender extends AbstractSender {
  private static final Logger LOG = LoggerFactory.getLogger(InfluxDbSender.class);
  private static final CharSequence MEDIA_TYPE_TEXT_PLAIN = HttpHeaders.createOptimized("text/plain");

  private final String metricsServiceUri;
  private final String database;


  private final Map<CharSequence, Iterable<CharSequence>> httpHeaders;
  private HttpClient httpClient;
  private final CharSequence auth;
  private String prefix;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx InflxuDb options
   * @param context the metric collection and sending execution context
   */
  public InfluxDbSender(Vertx vertx, VertxInfluxDbOptions options, Context context) {
    super(vertx, options, context);

    AuthenticationOptions authenticationOptions = options.getAuthenticationOptions();
    if (authenticationOptions.isEnabled()) {
      String authString = authenticationOptions.getUsername() + ":" + authenticationOptions.getSecret();
      try {
        auth = HttpHeaders.createOptimized("Basic " + Base64.getEncoder().encodeToString(authString.getBytes("UTF-8")));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      auth = null;
    }
    JsonObject optionsHttpHeaders = options.getHttpHeaders();
    if (optionsHttpHeaders != null) {
      this.httpHeaders = new HashMap<>(optionsHttpHeaders.size());
      for (String headerName : optionsHttpHeaders.fieldNames()) {
        CharSequence optimizedName = HttpHeaders.createOptimized(headerName);
        Object value = optionsHttpHeaders.getValue(headerName);
        List<String> values;
        if (value instanceof JsonArray) {
          values = ((JsonArray) value).stream().map(Object::toString).collect(toList());
        } else {
          values = Collections.singletonList(value.toString());
        }
        this.httpHeaders.put(optimizedName, values.stream().map(HttpHeaders::createOptimized).collect(toList()));
      }
    } else {
      this.httpHeaders = Collections.emptyMap();
    }

    metricsServiceUri = options.getMetricsServiceUri();

    database = options.getDatabase();
    prefix = options.getPrefix();
    
    context.runOnContext(aVoid -> {
      HttpClientOptions httpClientOptions = options.getHttpOptions()
          .setDefaultHost(options.getHost())
          .setDefaultPort(options.getPort());
      httpClient = vertx.createHttpClient(httpClientOptions);
    });
  }

  @Override
  protected void sendDataTo(String metricsDataUri, Object mixedData, String encoding) {
    if (mixedData instanceof Optional) {
      Optional<?> optional = (Optional<?>) mixedData;
      optional.filter(BatchPoints.class::isInstance).ifPresent(b -> {
        HttpClientRequest request = httpClient.post(metricsDataUri, this::onResponse)
            .exceptionHandler(err -> LOG.trace("Could not send metrics. Payload was: " + mixedData, err))
            .putHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE_TEXT_PLAIN);

          if (auth != null) {
            request.putHeader(HttpHeaders.AUTHORIZATION, auth);
          }
          httpHeaders.forEach(request::putHeader);
          String lineProtocol = ((BatchPoints)b).lineProtocol();
          if (LOG.isTraceEnabled()) {
            LOG.trace("Sending data to influxDb: \n" + lineProtocol);
          }
          request.end(lineProtocol, "UTF-8");
      });
    }
  }

  @Override
  protected void getMetricsDataUri(Handler<AsyncResult<String>> handler) {
      handler.handle(Future.succeededFuture(metricsServiceUri + "?db=" + database));
      return;
  }

  @Override
  protected Optional<BatchPoints> toMixedData(List<DataPoint> dataPoints) {
    BatchPoints batchPoints = BatchPoints.builder()
        .tag("async", "true")
        .build();

    Map<? extends Class<? extends DataPoint>, Map<String, List<DataPoint>>> mixedData;
    mixedData = dataPoints.stream().collect(groupingBy(DataPoint::getClass, groupingBy(DataPoint::getName)));
    addMixedData(batchPoints, "gauges", mixedData.get(GaugePoint.class));
    addMixedData(batchPoints, "counters", mixedData.get(CounterPoint.class));
    addMixedData(batchPoints, "availabilities", mixedData.get(AvailabilityPoint.class));
    if (batchPoints.getPoints().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(batchPoints);
  }
  
  private void addMixedData(BatchPoints batchPoints, String type, Map<String, List<DataPoint>> data) {
    if (data == null || data.isEmpty()) {
      return;
    }
    data.forEach((id, points) -> points.forEach(point-> batchPoints.getPoints().add(toPoint(point))));
  }
  
  private Point toPoint(DataPoint dataPoint) {
    Builder pointBuilder = Point.measurement(prefix)
          .time(dataPoint.getTimestamp(), TimeUnit.MILLISECONDS);
    if (dataPoint instanceof CounterPoint) {
      pointBuilder.addField(dataPoint.getName(), ((CounterPoint)dataPoint).getValue());
    } else if (dataPoint instanceof GaugePoint) {
      pointBuilder.addField(dataPoint.getName(), ((GaugePoint)dataPoint).getValue());
    } else {
      pointBuilder.addField(dataPoint.getName(), dataPoint.getValue().toString());
    }
    
    return pointBuilder.build();
  }

  private void onResponse(HttpClientResponse response) {
    if (response.statusCode() != 200 && LOG.isTraceEnabled()) {
      response.bodyHandler(msg -> {
        LOG.trace("Could not send metrics: " + response.statusCode() + " : " + msg.toString());
      });
    }
  }
 
  @Override
  public void stop() {
    super.stop();
    httpClient.close();
  }
}