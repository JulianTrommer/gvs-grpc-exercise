package de.unia.gvs.grpc.server;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.unia.gvs.grpc.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.logging.Logger;

public class PositionLogServer {
    private static final Logger log = Logger.getLogger(PositionLogServer.class.getSimpleName());

    private Server server;

    public void start() throws IOException {
        final int port = 4711;
        log.info("Starting server on port " + port);
        server = ServerBuilder.forPort(port)
                .addService(new PositionLogServiceImpl())
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

    static class PositionLogServiceImpl extends PositionLogServiceGrpc.PositionLogServiceImplBase {
        // Stores points for each user, identified by their ID
        private final Multimap<Integer, Coordinate> points = MultimapBuilder.treeKeys().arrayListValues().build();

        @Override
        public void getUsers(GetUsersRequest request, StreamObserver<GetUsersReply> responseObserver) {
            final GetUsersReply.Builder builder = GetUsersReply.newBuilder();
            points.keySet().forEach(builder::addUsersIds);

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void logPosition(LogPositionRequest request, StreamObserver<LogPositionReply> responseObserver) {
            points.putAll(request.getUserId(), request.getPointsList());
            responseObserver.onNext(LogPositionReply.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void getPoints(PointsRequest request, StreamObserver<Coordinate> responseObserver) {
            points.get(request.getUserId()).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        }
    }
}
