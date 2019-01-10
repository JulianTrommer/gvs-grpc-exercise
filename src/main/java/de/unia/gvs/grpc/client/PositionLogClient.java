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

    public List<Integer> listUsers() {
        return stub.listUsers(ListUsersRequest.getDefaultInstance()).getUsersIdsList();
    }

    public void removeUser(int userId) {
        stub.deleteUser(DeleteUserRequest.newBuilder().setUserId(userId).build());
    }

    public void logPoints(LogPositionRequest request) {
        stub.logPosition(request);
    }

    public LengthReply getTrackLength(int userId) {
        return stub.getTrackLength(LengthRequest.newBuilder().setUserId(userId).build());
    }
}
