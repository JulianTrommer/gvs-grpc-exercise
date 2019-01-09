package de.unia.gvs.grpc.server;

import de.unia.gvs.grpc.*;
import de.unia.gvs.grpc.PositionLogServiceGrpc.PositionLogServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ServerTest {
    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private Server server;
    private ManagedChannel channel;

    @Before
    public void setUp() throws IOException {
        final String name = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        final PositionLogServer.PositionLogServiceImpl service = new PositionLogServer.PositionLogServiceImpl();
        server = grpcCleanup.register(InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(service)
                .build()
                .start());

        // Create a client channel and register for automatic graceful shutdown.
        channel = grpcCleanup.register(InProcessChannelBuilder.forName(name).directExecutor().build());
    }

    @Test
    public void serviceImpl_getUsers() {
        final PositionLogServiceBlockingStub stub = PositionLogServiceGrpc.newBlockingStub(channel);
        final GetUsersReply reply = stub.getUsers(GetUsersRequest.getDefaultInstance());
        assertEquals("Newly created service should not return any users", 0, reply.getUsersIdsCount());
    }

    @Test
    public void serviceImpl_logPosition() {
        final Random random = new Random();
        final Coordinate point = Coordinate.newBuilder()
                .setLatitude(random.nextDouble() * 90)
                .setLongitude(random.nextDouble() * 180)
                .build();
        final LogPositionRequest request = LogPositionRequest.newBuilder()
                .setUserId(1234)
                .addPoints(point)
                .build();

        final PositionLogServiceBlockingStub stub = PositionLogServiceGrpc.newBlockingStub(channel);
        final LogPositionReply ignored = stub.logPosition(request);

        final List<Integer> idList = stub.getUsers(GetUsersRequest.getDefaultInstance()).getUsersIdsList();
        assertThat("User must exist after adding a track", idList, hasItem(request.getUserId()));

        final Iterator<Coordinate> points = stub.getPoints(PointsRequest.newBuilder().setUserId(request.getUserId()).build());
        assertThat("Track for user must contain logged point", () -> points, hasItem(point));
    }
}
