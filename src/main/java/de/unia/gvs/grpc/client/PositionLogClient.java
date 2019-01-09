package de.unia.gvs.grpc.client;

import de.unia.gvs.grpc.*;
import io.grpc.ManagedChannel;

import java.util.Iterator;
import java.util.List;

public class PositionLogClient {
    private PositionLogServiceGrpc.PositionLogServiceBlockingStub stub;

    public PositionLogClient(ManagedChannel channel) {
        this.stub = PositionLogServiceGrpc.newBlockingStub(channel);
    }

    public Iterator<Coordinate> getPoints(int userId) {
        final PointsRequest request = PointsRequest.newBuilder()
                .setUserId(userId)
                .build();
        return stub.getPoints(request);
    }

    public List<Integer> getUsers() {
        return stub.getUsers(GetUsersRequest.getDefaultInstance()).getUsersIdsList();
    }

    public void logPoints(LogPositionRequest request) {
        stub.logPosition(request);
    }
}
