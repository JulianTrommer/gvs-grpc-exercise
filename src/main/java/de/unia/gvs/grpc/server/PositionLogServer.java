package de.unia.gvs.grpc.server;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Empty;
import de.unia.gvs.grpc.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        public void listUsers(ListUsersRequest request, StreamObserver<ListUsersReply> responseObserver) {
            final ListUsersReply.Builder builder = ListUsersReply.newBuilder();
            points.keySet().forEach(builder::addUsersIds);

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
            if (!points.containsKey(request.getUserId())) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
                return;
            }

            points.removeAll(request.getUserId());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void logPosition(LogPositionRequest request, StreamObserver<Empty> responseObserver) {
            points.putAll(request.getUserId(), request.getPointsList());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void getPoints(PointsRequest request, StreamObserver<Coordinate> responseObserver) {
            points.get(request.getUserId()).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        }

        @Override
        public void getTrackLength(LengthRequest request, StreamObserver<LengthReply> responseObserver) {
            if (!points.containsKey(request.getUserId())) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
                return;
            }

            final List<Coordinate> points = new ArrayList<>(this.points.get(request.getUserId()));

            double distance = 0;
            for (int i = 0; i < points.size() - 1; ++i) {
                final Coordinate start = points.get(i);
                final Coordinate end = points.get(i + 1);

                final GeodesicData data = Geodesic.WGS84.Inverse(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());

                // s12 field is distance in millimeters
                distance += data.s12 / 1000.0;
            }

            final LengthReply.Builder reply = LengthReply.newBuilder();
            reply.setNumPoints(points.size());
            reply.setLength(distance);

            responseObserver.onNext(reply.build());
            responseObserver.onCompleted();
        }
    }
}
