package de.unia.gvs.undertow;

import com.google.common.collect.Streams;
import de.unia.gvs.grpc.HelloReply;
import de.unia.gvs.grpc.HelloRequest;
import de.unia.gvs.undertow.client.HelloClient;
import de.unia.gvs.undertow.server.HelloServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) throws IOException {
        final HelloServer helloServer = new HelloServer();
        helloServer.start();

        final ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 4711)
                .usePlaintext()
                .build();
        final HelloClient client = new HelloClient(channel);

        final RoutingHandler routingHandler = new RoutingHandler()
                .get("/hello/{count}", exchange -> {
                    final int count = Integer.parseInt(exchange.getQueryParameters().get("count").getFirst());
                    final HelloRequest req = HelloRequest.newBuilder()
                            .setName("Foobar")
                            .setCount(count)
                            .build();

                    final Iterator<HelloReply> data = client.fetchData(req);
                    final String msg = Streams.stream(data)
                            .map(HelloReply::getMessage)
                            .collect(Collectors.joining("\n"));

                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send(msg);
                });

        final Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(routingHandler)
                .build();
        server.start();
    }
}
