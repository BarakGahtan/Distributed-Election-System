package state;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import protos.VotingServerGRPC;

public class Connection {
    public String grpcHostPort;
    ManagedChannel channel;
    VotingServerGRPC.VotingServerFutureStub stub;
    VotingServerGRPC.VotingServerBlockingStub blockingStub;


    Connection(String grpcHostPort) {
        this.grpcHostPort = grpcHostPort;
        channel = getNewChannel(grpcHostPort);
        stub = getNewStub(channel);
        blockingStub = getNewBlockingStub(channel);
    }

    void close() {
        channel.shutdown();
    }

    private ManagedChannel getNewChannel(String grpcHostPort) {
        return ManagedChannelBuilder
                .forTarget(grpcHostPort)
                // TODO: do we really need the Decorator?
//                .intercept(new Decorator(0))
                .usePlaintext()
                .build();
    }

    private VotingServerGRPC.VotingServerFutureStub getNewStub(ManagedChannel c) {
        return VotingServerGRPC.newFutureStub(c);
    }

    private VotingServerGRPC.VotingServerBlockingStub getNewBlockingStub(ManagedChannel c) {
        return VotingServerGRPC.newBlockingStub(c);
    }
}
