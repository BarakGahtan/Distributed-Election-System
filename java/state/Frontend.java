package state;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protos.VotingServerGRPC;
import protos.VotingServerOuterClass;

import rest.ClientVoteRequest;
import protos.VotingServerOuterClass.VoteRequest;


import rest.ConvertToReportForState;
import zk.ZookeeperServer;
import zk.SystemProperties;

import java.io.IOException;
import java.util.concurrent.Future;

import static java.util.Arrays.sort;


enum ElectionPhase {
    WAITING, RUNNING, ENDING
}

public class Frontend extends VotingServerGRPC.VotingServerImplBase {
    private static Logger log = LoggerFactory.getLogger(Frontend.class);
    static SystemProperties prop;
    Server grpcServer;
    public int shardNum;
    public String grpcHostPort;
    final static String voterDataBaseFilename = "voterDB.txt";
    final static String nomineesFilename = "nominees.txt";
    PrintWriter voteResultsOut;
    ZookeeperServer zookeeperServer;
    private ElectionPhase phase;
    public final List<ArrayList<Connection>> connections;
    public final Map<Integer, Integer> finalVotes;
    public boolean committee = false;
    private boolean published = false;
    private int nomineeCount;

    public Frontend(SystemProperties prop, int shardNum, String grpcHostPort)
            throws IOException {
        Frontend.prop = prop;
        this.shardNum = shardNum;
        committee = Integer.parseInt(grpcHostPort.split(":")[1]) == prop.serverCount() + 10000;
        if (!committee) {
            this.zookeeperServer = new ZookeeperServer(this, prop, shardNum);
        } else {
            log.info("I'm the committee server");
        }
        this.phase = ElectionPhase.WAITING;
        this.connections = new ArrayList<>(prop.shardCount);
        for (int i = 0; i < prop.shardCount; i++)
            connections.add(i, new ArrayList<>());
        this.finalVotes = new HashMap<Integer, Integer>();
        this.grpcHostPort = grpcHostPort;
    }

    public Frontend init() {
        if (!committee) {
            try {
                zookeeperServer.init(grpcHostPort);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return this;
    }

    public void run() {
        try {
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncVotes(byte[] data) {
        VoteRequest req;
        try {
            req = VoteRequest.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return;
        }

        synchronized (finalVotes) {
            finalVotes.put(req.getVoterID(), req.getNominee());
        }
    }

    public String vote(int nominee, int voterId) {
        if (phase == ElectionPhase.WAITING)
            return "Election has not started yet!";
        if (phase == ElectionPhase.ENDING)
            return "Election has ended!";
        if (nominee >= nomineeCount)
            return "There is no such nominee!";

        int shard = findVoterShard(voterId);
        if (shard == -1)
            return "Voter does not exists in the system.";

        VoteRequest vt = new ClientVoteRequest(Integer.parseInt(grpcHostPort.split(":")[1]), nominee, voterId).convertToVoteRequest();
        if (shard == shardNum) {
            byte[] data = vt.toByteArray();
            zookeeperServer.pushTransaction(data);
        } else {
            System.out.println("Forwarding to another shard:");
            synchronized (connections) {
                try {
                    connections.get(shard).get(0).stub.TransferVoteRequest(vt);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return "Ack";
    }

    private int winningNomineeInShard() {
        int[] results = new int[nomineeCount];

        synchronized (finalVotes) {
            for (Map.Entry<Integer, Integer> entry : finalVotes.entrySet()) {
                Integer voter = entry.getKey();
                Integer nominee = entry.getValue();
                log.info("voter " + voter + "to nominee " + nominee);
                results[nominee]++;
            }
            // finalVotes.forEach((voter, nominee) -> results[nominee]++);
        }

        int resId = 0, resVotes = results[0];
        for (int i = 1; i < nomineeCount; i++) {
            if (results[i] > resVotes) {
                resVotes = results[i];
                resId = i;
            }
        }

        return resId;
    }

    public Integer winningNominee() {
        if (phase == ElectionPhase.WAITING)
            return -2;

        int current_state = 0;
        int i = 0;
        int[] result = new int[prop.shardCount];
        while (current_state < prop.shardCount) {
            VotingServerOuterClass.StateID stateid = new ConvertToReportForState(current_state).ConvertToReportForState();
            int j;
            for (j = 0; j < prop.serversPerShard; j++) {
                Integer grpcPort = 10000 + j + current_state * prop.serversPerShard;
                String grpcHostPort = "127.0.0.1:" + grpcPort.toString();
                log.info(grpcHostPort);
                Connection c = new Connection(grpcHostPort);
                VotingServerOuterClass.ReportForState report;
                try {
                    report = c.blockingStub.ReportPerState(stateid);
                    int nominee = report.getNominee();
                    result[current_state] = nominee;
                    log.info(nominee + " won for shard " + current_state);
                } catch (Exception e) {
                    log.info("cannot connect to server: " + j + " in shard " + current_state);
                    continue;
                }
                c.close();
                log.info("connect to server: " + j + " in shard " + current_state);
                break;
            }
            if (j == prop.serversPerShard) {
                log.info("didnt find a connection to that shard");
            }
            current_state += 1;
            i++;
        }

        int[] winner = new int[nomineeCount];
        for (int j = 0; j < nomineeCount; j++)
            winner[j] = 0;
        for (int j = 0; j < prop.shardCount; j++) {
            winner[result[j]] += 1;
        }
        sort(winner, 0, nomineeCount);

        if (winner[2] == winner[1])
            return -1;

        return winner[2];
    }

    public void startElections(String voterDbPath) {
        if (!zookeeperServer.finishedInit)
            return;
        if (phase != ElectionPhase.WAITING || phase == ElectionPhase.RUNNING)
            return;

        setPhaseRunning();
        nomineeCount = getNomineeCount();
        if (nomineeCount == -1) {
            log.info("could not get the nominees number");
            return;
        }
        log.info("the number of nominees is " + nomineeCount);
        int shard = 0;

        log.info("Number of shards(connection):" + zookeeperServer.shardsServers.size());

        for (List<String> shardServers : zookeeperServer.shardsServers) {
            log.info("servers: " + shardServers.size());
            synchronized (connections) {
                for (String hostPort : shardServers) {
                    connections.get(shard).add(new Connection(hostPort));
                    log.info("Connected to gRPC address: " + hostPort);
                }
            }
            shard++;
        }

        zookeeperServer.broadcastStart(voterDbPath);
    }

    public void removeConnection(int shard, String hostPort) {
    }

    public void setPhaseRunning() {
        phase = ElectionPhase.RUNNING;
    }

    public void endElections() {
        if (phase != ElectionPhase.RUNNING)
            return;

        setPhaseEnding();
        zookeeperServer.broadcastEnd();
    }

    public void setPhaseEnding() {
        phase = ElectionPhase.ENDING;
    }

    public int findVoterShard(int voter) {
        try {
            if (!Files.exists(Paths.get(voterDataBaseFilename)))
                System.out.println("Frontend.findVoterState(): votersDb doesn't exists.");

            List<String> lines = Files.readAllLines(Paths.get(voterDataBaseFilename), StandardCharsets.US_ASCII);
            for (String line : lines) {
                String[] tokens = line.split(", ");
                if (!tokens[0].equals("#") && Integer.parseInt(tokens[0]) == voter)
                    return Integer.parseInt(tokens[1]);

            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        return -1;
    }

    public int getNomineeCount() {
        try {
            if (!Files.exists(Paths.get(nomineesFilename))) {
                log.info("getNomineeCount(): " + nomineesFilename + " doesn't exists.");
                return -1;
            }

            return Files.readAllLines(Paths.get(nomineesFilename), StandardCharsets.US_ASCII).size() - 1;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public int sendGRPCRequest(int shard, int server, int req) {
        int result;
        Integer grpcPort = 10000 + server + shard * prop.serversPerShard;
        String grpcHostPort = "127.0.0.1:" + grpcPort.toString();
        Connection c = new Connection(grpcHostPort);
        VotingServerOuterClass.ReportForState report;
        try {
            result = c.blockingStub.ReportPerState(new ConvertToReportForState(req).ConvertToReportForState()).getNominee();
        } catch (Exception e) {
            log.info("cannot connect to server: " + server + " in shard " + shard);
            result = -1;
        }

        c.close();
        return result;
    }

    public int committeeShardReport(int shard) {
        int result = -2;

        if (phase == ElectionPhase.WAITING)
            return result;

        for (int j = 0; j < prop.serversPerShard; j++) {
            log.info("sending report request to server " + j + " in shard " + shard);
            result = sendGRPCRequest(shard, j, 0);
            log.info("result: " + result);
            if (result >= 0) break;
        }

        return result;
    }

    public int committeeStartElections(String db) {
        if (phase != ElectionPhase.WAITING)
            return -2;
        phase = ElectionPhase.RUNNING;
        nomineeCount = getNomineeCount();
        log.info("The number of nominees is " + nomineeCount);
        for (int i = 0; i < prop.shardCount; i++) {
            for (int j = 0; j < prop.serversPerShard; j++) {
                if (sendGRPCRequest(i, j, -1) == 0)
                    return 0;
            }
        }
        log.info("Error committee start elections");
        return 0;
    }

    public int committeeEndElections() {
        if (phase != ElectionPhase.RUNNING)
            return -1;
        phase = ElectionPhase.ENDING;

        for (int i = 0; i < prop.shardCount; i++) {
            for (int j = 0; j < prop.serversPerShard; j++) {
                if (sendGRPCRequest(i, j, -2) == 0)
                    return 0;
            }
        }
        log.info("Error committee end elections");
        return 0;
    }

    public void committeePublish() {
        if (published = true)
            return;


        published = true;
    }

    @Override
    public void TransferVoteRequest(protos.VotingServerOuterClass.VoteRequest request,
                                    io.grpc.stub.StreamObserver<protos.VotingServerOuterClass.STATUS> responseObserver) {
        log.info("got a vote request from server with gRPC port: " + request.getFromServer());
        byte[] data = request.toByteArray();
        zookeeperServer.pushTransaction(data);
        VotingServerOuterClass.STATUS rep =
                VotingServerOuterClass.STATUS.newBuilder()
                        .setSUCCESS(0)
                        .build();
        responseObserver.onNext(rep);
        responseObserver.onCompleted();
    }

    @Override
    public void ReportPerState(VotingServerOuterClass.StateID stateId,
                               io.grpc.stub.StreamObserver<VotingServerOuterClass.ReportForState> responseObserver) {
        VotingServerOuterClass.ReportForState rep = null;
        // state.getNominee is equally the requested shard id;
        if (stateId.getNominee() == -1) {
            startElections("voterDb.txt");
            rep = VotingServerOuterClass.ReportForState.newBuilder()
                    .setNominee(0)
                    .build();
        } else if (stateId.getNominee() == -2) {
            endElections();
            rep = VotingServerOuterClass.ReportForState.newBuilder()
                    .setNominee(0)
                    .build();
        } else {
            log.info("checking the current winning candidate");
            log.info("nominee count: " + nomineeCount);
            int winner = winningNomineeInShard();
            rep = VotingServerOuterClass.ReportForState.newBuilder()
                    .setNominee(winner)
                    .build();
            log.info(winner + " won");
        }

        responseObserver.onNext(rep);
        responseObserver.onCompleted();
    }


    private Server getNewServer(int port) {
        try {
            return ServerBuilder.forPort(port)
                    .addService(this)
                    //           .intercept(new Interceptor())
                    .build()
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }

    }

    void startServer() throws IOException {
        /* The port on which the server should run */
        int port = Integer.parseInt(grpcHostPort.split(":")[1]);
        grpcServer = getNewServer(port);
        log.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            Frontend.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    public void stop() {
        synchronized (connections) {
            for (ArrayList<Connection> l : connections) {
                for (Connection c : l) {
                    c.close();
                }
            }
        }

        if (grpcServer != null) {
            grpcServer.shutdown();
        }
    }

    void blockUntilShutdown() throws InterruptedException {
        log.info("blockUntilShutdown()");
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }
}




























