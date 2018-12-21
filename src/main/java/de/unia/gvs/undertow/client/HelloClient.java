package de.unia.gvs.undertow.client;

import de.unia.gvs.grpc.HelloReply;
import de.unia.gvs.grpc.HelloRequest;
import de.unia.gvs.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;

import java.util.Iterator;

public class HelloClient {
    private ManagedChannel channel;
    private HelloServiceGrpc.HelloServiceBlockingStub stub;

    public HelloClient(ManagedChannel channel) {
        this.channel = channel;
        this.stub = HelloServiceGrpc.newBlockingStub(channel);
    }

    public Iterator<HelloReply> fetchData(HelloRequest req) {
        return stub.sayHello(req);
    }
}
