package de.unia.gvs.undertow.server;

import de.unia.gvs.grpc.HelloReply;
import de.unia.gvs.grpc.HelloRequest;
import de.unia.gvs.grpc.HelloServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opencensus.common.Scope;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.samplers.Samplers;

import java.io.IOException;
import java.util.logging.Logger;

public class HelloServer {
    private static final Logger log = Logger.getLogger(HelloServer.class.getSimpleName());
    private static final Tracer tracer = Tracing.getTracer();

    private Server server;

    public void start() throws IOException {
        setupOpencensusAndExporters();

        final int port = 4711;
        log.info("Starting server on port " + port);
        server = ServerBuilder.forPort(port)
                .addService(new HelloServiceImpl())
                .build();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void stop() {
        if (server != null) {
            log.info("Shutting down server");
            server.shutdown();
            log.info("Done.");
        }
    }

    private void setupOpencensusAndExporters() throws IOException {
        TraceConfig traceConfig = Tracing.getTraceConfig();
        traceConfig.updateActiveTraceParams(
                traceConfig.getActiveTraceParams()
                        .toBuilder()
                        .setSampler(Samplers.alwaysSample())
                        .build());

        // Tracing
        JaegerTraceExporter.createAndRegister("http://127.0.0.1:14268/api/traces", "hello-service");

        // Register all the gRPC views and enable stats
        RpcViews.registerAllGrpcViews();
    }

    static class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            final HelloReply reply = HelloReply.newBuilder().setMessage("Hello, " + request.getName()).build();

            for (int i = 0; i < request.getCount(); ++i) {
                responseObserver.onNext(reply);
            }
            responseObserver.onCompleted();
        }
    }
}
