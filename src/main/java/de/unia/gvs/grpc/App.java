package de.unia.gvs.grpc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import com.diffplug.common.base.Errors;
import com.google.common.collect.Streams;
import com.google.protobuf.util.JsonFormat;

import org.jboss.logging.Logger;

import de.unia.gvs.grpc.client.PositionLogClient;
import de.unia.gvs.grpc.server.PositionLogServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicates;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.JBossLoggingAccessLogReceiver;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.Headers;

import static java.util.stream.Collectors.joining;

public class App {
    private static final Logger log = Logger.getLogger(App.class);
    private static final int HTTP_PORT = 8080;

    public static void main(String[] args) throws IOException {
        log.info("Starting gRPC server");
        final PositionLogServer positionLogServer = new PositionLogServer();
        positionLogServer.start();

        log.info("Starting gRPC client");
        final ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 4711)
                .usePlaintext()
                .build();
        final PositionLogClient client = new PositionLogClient(channel);

        // Add some sample data
        log.info("Adding sample data");
        createSampleData(client);

        log.info("Starting Undertow HTTP server");
        final Undertow server = initUndertow(client);
        server.start();
        log.info("HTTP server running on http://localhost:" + HTTP_PORT);
    }

    private static void createSampleData(PositionLogClient client) {
        for (int userId = 1; userId < 5; ++userId) {
            final double centroid_lat = 48.333889;
            final double centroid_lon = 10.898333;
            final Random random = new Random();

            final LogPositionRequest.Builder builder = LogPositionRequest.newBuilder().setUserId(userId);
            for (int i = 0; i < 15; ++i) {
                final Coordinate point = Coordinate.newBuilder()
                        .setLatitude(centroid_lat + random.nextGaussian() * 0.01)
                        .setLongitude(centroid_lon + random.nextGaussian() * 0.01)
                        .build();
                builder.addPoints(point);
            }
            client.logPoints(builder.build());
        }
    }

    private static Undertow initUndertow(PositionLogClient client) {
        final RoutingHandler routingHandler = new RoutingHandler()
                .get("/", Handlers.redirect("/map.html"))
                .get("/points/{userId}", exchange -> {
                    final int userId = Integer.parseInt(exchange.getQueryParameters().get("userId").getFirst());

                    final Iterator<Coordinate> data = client.getPoints(userId);
                    final String json = Streams.stream(data)
                            .map(Errors.rethrow().wrap(JsonFormat.printer()::print))
                            .collect(joining(",\n"));

                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
                    exchange.getResponseSender().send("[" + json + "]");
                })
                .get("/users", exchange -> {
                    final String json = client.getUsers()
                            .stream()
                            .map(Object::toString)
                            .collect(joining(",\n"));
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
                    exchange.getResponseSender().send("[" + json + "]");
                })
                .setFallbackHandler(exchange -> {
                    exchange.setStatusCode(404);
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("404 -- page not found");
                });

        final ResourceManager resourceManager = new ClassPathResourceManager(App.class.getClassLoader(), "web");
        final ResourceHandler resourceHandler = new ResourceHandler(resourceManager);

        final PredicateHandler handler = new PredicateHandler(Predicates.suffixes(".html", ".js", ".css"), resourceHandler, routingHandler);
        final AccessLogHandler accessLogHandler = new AccessLogHandler(handler, new JBossLoggingAccessLogReceiver(), "combined", App.class.getClassLoader());

        return Undertow.builder()
                .addHttpListener(HTTP_PORT, "0.0.0.0")
                .setHandler(accessLogHandler)
                .build();
    }
}
