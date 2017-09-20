package io.vertx.ext.metric.reporters.influxdb.impl;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.metric.reporters.influxdb.AuthenticationOptions;
import io.vertx.ext.metric.reporters.influxdb.VertxInfluxDbOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class InfluxDbSenderTest {

  private Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new VertxInfluxDbOptions().setAuthenticationOptions(
        new AuthenticationOptions().setUsername("xx").setSecret("yy").setEnabled(true))
        .setEnabled(true).setPrefix("vert.x"))
    );

  }

  @After
  public void after(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void shouldSendDataToInfluxDb(TestContext context) {
    Async async = context.async();
    vertx.createHttpServer(new HttpServerOptions()
      .setCompressionSupported(true)
      .setDecompressionSupported(true)
      .setLogActivity(true)
      .setHost("localhost")
      .setPort(8086))
      .requestHandler(req -> {
        req.exceptionHandler(t -> System.out.println("An exception occured handling request: " + t));

        Buffer fullRequestBody = Buffer.buffer();
        req.handler(body -> {
          fullRequestBody.appendBuffer(body);
        });
        req.endHandler(h -> {
          context.assertTrue(fullRequestBody.toString().contains("eventbus.publishedRemoteMessages=0i"));
          req.response().setStatusCode(200).end();
          async.complete();
        });
      }).listen(8086, "localhost", context.asyncAssertSuccess());
    async.awaitSuccess();
  }
}
