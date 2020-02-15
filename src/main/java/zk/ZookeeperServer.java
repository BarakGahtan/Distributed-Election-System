package zk;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protos.VotingServerOuterClass.VoteRequest;
import com.google.protobuf.InvalidProtocolBufferException;

import state.Connection;
import state.Frontend;

import zk.OurUtils;

// check if the transaction type is reportRequest.
// should we enable to view report of another shard? is it the committee responsibility to send a report request to each shard?

public class ZookeeperServer implements Watcher {
    private static Logger log = LoggerFactory.getLogger(ZookeeperServer.class);
    private ZooKeeper zk;
    private SystemProperties prop;
    private String shard;
    private String serverNode;
    private Frontend frontend;
    //
    private String currTransactionNode;
    private String prevVoteTransactionNode;
    private long currTransaction, prevTransaction, lastTransaction;
    private byte[] currTransactionData;
    boolean syncMode = false;
    //
    private int liveServers;
    private String serversLeader;
    private boolean isLeader;
    private boolean electionsBarrier;
    public final ArrayList<ArrayList<String>> shardsServers;
    public final Map<String, String> nodesData;
    public boolean finishedInit;
    //
    private int liveShardServers;
    private String shardLeader;
    private boolean isShardLeader;
    //
    public final Map<Integer, Integer> votes;
    //
    private Integer mutex;

    public ZookeeperServer(Frontend frontend, SystemProperties prop, int shardNum) {
        this.frontend = frontend;
        this.prop = prop;
        this.shard = Integer.toString(shardNum);
        this.votes = new HashMap<Integer, Integer>();
        this.electionsBarrier = true;
        this.nodesData = new HashMap<>();
        shardsServers = new ArrayList<>(prop.shardCount);
        for (int i = 0; i < prop.shardCount; i++)
            shardsServers.add(i, new ArrayList<String>());
        this.mutex = new Integer(-1);
        finishedInit = false;
        lastTransaction = -1;
    }

    private String createNode(String path, byte[] data, CreateMode createMode)
            throws KeeperException, InterruptedException {
        boolean exists = false;
        try {
            zk.exists(path, false);
        } catch (NodeExistsException e) {
            exists = true;
        }

        String createdPath = null;
        if (!exists) {
            try {
                createdPath = zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
            } catch (NodeExistsException e) {
            }
        }

        return createdPath;
    }

    public ZookeeperServer init(String data)
            throws KeeperException, InterruptedException {
        try {
            zk = new ZooKeeper(prop.zookeeperAddress, 3000, this);
        } catch (IOException e) {
            e.printStackTrace();
            zk = null;
            return null;
        }

        /* assuming failure only after election start */
        boolean created = createNode(prop.rootPath(), null, CreateMode.CONTAINER) != null;
        zk.getData(prop.rootPath(), true, null);
        if (created) {
            log.info("Created root node");
        }
        createNode(serversReadyPath(), null, CreateMode.CONTAINER);

        String serverData = new String(shard.toString() + "," + data);
        String serverPath = createNode(serversReadyPath() + "/" + prop.serverBaseName,
                (shard + "," + data).getBytes(),
                CreateMode.EPHEMERAL_SEQUENTIAL);
        String[] nodes = serverPath.split("/");
        serverNode = nodes[nodes.length - 1];

        while (true) {
            synchronized (mutex) {
                List<String> children = zk.getChildren(serversReadyPath(), true);
                log.info("ready: children.size()=" + children.size() + ", serverCount()=" + prop.serverCount());
                if (children.size() < prop.serverCount()) {
                    mutex.wait();
                } else {
                    break;
                }
            }
        }
        log.info("all joined ready path, copying hosts data:");
        getHostsAndSelectLeader(serversReadyPath(), zk.getChildren(serversReadyPath(), false));

        createNode(serversPath(), null, CreateMode.CONTAINER);
        log.info("create node in servers path: " + createNode(serversPath() + "/" + serverNode, null, CreateMode.EPHEMERAL));
        List<String> serversChildren;
        while (true) {
            synchronized (mutex) {
                serversChildren = zk.getChildren(serversPath(), true);
                log.info("servers: children.size()=" + serversChildren.size() + ", serverCount()=" + prop.serverCount());
                if (serversChildren.size() < prop.serverCount()) {
                    mutex.wait();
                } else {
                    break;
                }
            }
        }

        try {
            zk.removeWatches(serversReadyPath(), this, WatcherType.Any, true);
        } catch (KeeperException.NoWatcherException e) {}
        if (isLeader) {
            log.info("Deleting the barrier");
            deleteParent(serversReadyPath(), zk.getChildren(serversReadyPath(), false));
        }

        createNode(shardPath(), null, CreateMode.CONTAINER);
        createNode(shardServersPath(), null, CreateMode.CONTAINER);
        createNode(transactionsPath(), null, CreateMode.PERSISTENT);
        zk.getChildren(transactionsPath(), true);
        String shardServerPath = createNode(shardServersPath() + "/" + serverNode,
                null,
                CreateMode.EPHEMERAL);
        log.info("created shard node in path: " + shardServerPath);

        while (true) {
            synchronized (mutex) {
                List<String> children = zk.getChildren(shardServersPath(), true);
                log.info("shard: children.size()=" + children.size() + ", serverCount()=" + prop.serversPerShard);
                if (children.size() < prop.serversPerShard) {
                    mutex.wait();
                } else {
                    break;
                }
            }
        }
        log.info("all joined shard path, starting shard leader election");
        shardLeaderElection();
        zk.getChildren(serversPath(), true);
        finishedInit = true;

        return this;
    }

    synchronized public void process(WatchedEvent event) {
        log.debug("New watch event " + event);
        watchEvent:
        try {
            String path = event.getPath();
            Event.EventType eventType = event.getType();
            if (eventType == Event.EventType.None) {
                // We are are being told that the state of the
                // connection has changed
                switch (event.getState()) {
                    case SyncConnected:
                        break;
                    case Expired:
                        // TODO:
                        assert (false);
                        break watchEvent;
                }
            } else if (path != null) {
                String[] nodes = path.split("/");
                String node = nodes[nodes.length - 1];
                String parent = null;
                if (nodes.length > 1)
                    parent = nodes[nodes.length - 2];

                if (path.equals(prop.rootPath())) {
                    // log.info("Root path watch event");
                    if (eventType.equals(Event.EventType.NodeDataChanged)) {
                        // log.info("Root path watch event with event type NodeDataChanged");
                        // Election state and reporting mechanism
                        byte[] raw = zk.getData(prop.rootPath(), true, null);
                        if (raw != null) {
                            String state = new String(raw, Charset.defaultCharset());
                            if (state.equals("start")) {
                                log.info("Election state changed: Start");
                                frontend.startElections("voterDb.txt");
                            } else if (state.equals("end")) {
                                log.info("Election state changed: End");
                                frontend.setPhaseEnding();
                            } else {
                                log.info("Got root path event with unknown broadcast message!");
                            }
                        }
                    }
                }
                if (path.equals(serversPath())) {
                    if (eventType.equals(Event.EventType.NodeChildrenChanged)) {
                        if (finishedInit == false) {
                            synchronized (mutex) {
                                mutex.notify();
                            }
                            break watchEvent;
                        }

                        serversLeaderElection();
                        zk.getChildren(serversPath(), true);
                    }
                }
                if (path.equals(serversReadyPath())) {
                    if (eventType.equals(Event.EventType.NodeChildrenChanged)) {
                        synchronized (mutex) {
                            mutex.notify();
                            break watchEvent;
                        }
                    }
                }
                if (path.equals(shardServersPath())) {
                    if (eventType.equals(Event.EventType.NodeChildrenChanged)) {
                        if (finishedInit == false) {
                            synchronized (mutex) {
                                mutex.notify();
                            }
                            break watchEvent;
                        }

                        shardLeaderElection();
                        zk.getChildren(shardServersPath(), true);
                        handleTransaction();
                    }
                }
                if (path.equals(shardReportRequestPath())) {
                    // the leader is the one who keeps every one in sync
                    if (eventType.equals(Event.EventType.NodeCreated) && isShardLeader) {
                        // TODO: gRPC
                    }
                }
                if (node.equals(currTransactionNode)) {
                    if (eventType.equals(Event.EventType.NodeChildrenChanged) && isShardLeader) {
                        List<String> committed;
                        try {
                            committed = zk.getChildren(path, true);
                        } catch (KeeperException.NoNodeException e) {
                            return;
                        }

                        if (committed.size() < liveShardServers) {
                            log.info("transactions: " + committed.size() + " live: " + liveShardServers);
                            return;
                        }

                        try {
                            zk.removeWatches(path, this, WatcherType.Any, true);
                        } catch (Exception e) {
                            log.error("Trying removing watches from node without any watches!");
                        }
                        deleteParent(path, committed); // sets a watch event on transactionsPath so no need to call handleTransactions() here
                        log.info("Deleted the oldest transaction: " + currTransactionNode);
                    }
                }
                if (path.equals(transactionsPath())) {
                    if (eventType.equals(Event.EventType.NodeChildrenChanged)) {
                        handleTransaction();
                    }
                }
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        notifyAll();
    }

    private void handleTransaction() throws KeeperException, InterruptedException {
        List<String> transactions = zk.getChildren(transactionsPath(), true);
        if (transactions.size() == 0)
            return;

        String minTransaction = transactions.get(0);
        int minTransactionIdx = Integer.parseInt(minTransaction.substring(SystemProperties.transactionBaseName.length()));
        for (String child : transactions) {
            int childNum = Integer.parseInt(child.substring(SystemProperties.transactionBaseName.length()));
            if (childNum < minTransactionIdx) {
                minTransaction = child;
                minTransactionIdx = childNum;
            }
        }

        if (lastTransaction == -1 || lastTransaction < minTransactionIdx) {
            currTransactionNode = minTransaction;
            log.info("Found the oldest transaction: " + currTransactionNode);
            byte[] data = zk.getData(transactionsPath() + "/" + currTransactionNode, true, null);
            currTransactionData = data;
            currTransaction = minTransactionIdx;
            frontend.syncVotes(data);
            lastTransaction = minTransactionIdx;
            zk.exists(transactionsPath() + "/" + currTransactionNode, true);
            createNode(transactionsPath() + "/" + currTransactionNode + "/" + serverNode,
                    null, CreateMode.EPHEMERAL);
            log.info("Committed the oldest transaction: " + currTransactionNode);

            if (isShardLeader) {
                String currTransactionNodePath = transactionsPath() + "/" + currTransactionNode;
                List<String> committed = zk.getChildren(transactionsPath() + "/" + currTransactionNode, true);
                if (committed.size() < liveShardServers) {
                    log.info("transactions: " + committed.size() + " live: " + liveShardServers);
                    return;
                }

                deleteParent(transactionsPath() + "/" + currTransactionNode, committed);
                 log.info("Deleted the oldest transaction: " + currTransactionNode);
            }

        }
    }

    private void deleteParent(String parentPath, List<String> children)
            throws KeeperException, InterruptedException {
        for (String child : children)
            zk.delete(parentPath + "/" + child, -1);
        zk.delete(parentPath, -1);
    }

    public void pushTransaction(byte[] data) {
        try {
            log.info("Pushed transaction: " + createNode(transactionsPath() + "/" + SystemProperties.transactionBaseName, data, CreateMode.PERSISTENT_SEQUENTIAL));
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void requestReport() {
        try {
            zk.exists(shardReportRequestPath(), false);
        } catch (KeeperException.NoNodeException e) {
            try {
                zk.create(prop.rootPath(), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (Exception ep) {
                ep.printStackTrace();
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public void watchReportRequests() {
        try {
            zk.exists(shardReportRequestPath(), true);
        } catch (KeeperException.NoNodeException e) {
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public void broadcastStart(String voterDbPath) {
        try {
            log.info("Broadcasting start");
            zk.setData(prop.rootPath(), "start".getBytes(), -1);
        } catch (KeeperException e) {
            System.out.println("broadcastStart: " + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception" + e);
        }
    }

    public void broadcastEnd() {
        try {
            log.info("Broadcasting end");
            zk.setData(prop.rootPath(), "end".getBytes(), -1);
        } catch (KeeperException e) {
            System.out.println("ZookeeperServer.enter() when creating a znode: " + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception" + e);
        }
    }

    private void shardLeaderElection() throws KeeperException, InterruptedException {
        log.info("Entering shard leader election.");
        List<String> children = zk.getChildren(shardServersPath(), true);

        // Shard leader election
        liveShardServers = children.size();
        shardLeader = serverNode;
        int serverId = Integer.parseInt(shardLeader.substring(SystemProperties.serverBaseName.length()));
        int leaderId = serverId;
        for (String child : children) {
            int childId = Integer.parseInt(child.substring(SystemProperties.serverBaseName.length()));
            if (childId < leaderId) {
                leaderId = childId;
                shardLeader = child;
            }
        }

        isShardLeader = serverId == leaderId;
        if (isShardLeader) {
            log.info("I'm the new shard leader");
            watchReportRequests();
        }
    }

    private List<String> serversLeaderElection() throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(serversPath(), true);

        synchronized (frontend.connections) {
            if (nodesData.size() != children.size()) {
                String serverToRemove = null;
                Map.Entry<String, String> i = null;
                for (Map.Entry<String, String> e : nodesData.entrySet()) {
                    String server = e.getKey();
                    if (!children.contains(server)) {
                        serverToRemove = server;
                        i = e;
                        break;
                    }
                }
                if (serverToRemove != null) {
                    int shard = Integer.parseInt(i.getValue().split(",")[0]);
                    String hostPort = i.getValue().split(",")[1];
                    log.info("removing server from data: " + serverToRemove);
                    shardsServers.get(shard).remove(hostPort);
                    nodesData.remove(serverToRemove);
                    List<state.Connection> l = frontend.connections.get(shard);
                    Iterator<Connection> iter = l.iterator();
                    while (iter.hasNext()) {
                        Connection c = iter.next();
                        if (c.grpcHostPort.equals(hostPort)) {
                            iter.remove();
                            log.info("removing server from connections");
                            break;
                        }
                    }
                }
            }
        }

        // Servers leader election
        assert (serverNode != null);
        serversLeader = serverNode;
        int serverId = Integer.parseInt(serversLeader.substring(SystemProperties.serverBaseName.length()));
        int leaderId = serverId;
        for (String child : children) {
            int childId = Integer.parseInt(child.substring(SystemProperties.serverBaseName.length()));
            if (childId < leaderId) {
                leaderId = childId;
                serversLeader = child;
            }
        }

        isLeader = serverId == leaderId;
        if (isLeader)
            log.info("I'm the leader");
        liveServers = children.size();
        return children;
    }

    public void getHostsAndSelectLeader(String barrier, List<String> children) throws KeeperException, InterruptedException {
        serversLeader = serverNode;
        int serverId = Integer.parseInt(serversLeader.substring(SystemProperties.serverBaseName.length()));
        int leaderId = serverId;
        log.info("children.size(): " + children.size());
        for (String child : children) {
            int childId = Integer.parseInt(child.substring(SystemProperties.serverBaseName.length()));
            if (childId < leaderId) {
                leaderId = childId;
                serversLeader = child;
            }

            log.info("reading data of " + child);
            byte[] raw = zk.getData(barrier + "/" + child, false, null);
            if (raw != null) {
                String serverData = new String(raw, Charset.defaultCharset()); // shard,hostPort
                String[] tokens = serverData.split(",");
                int shard = Integer.parseInt(tokens[0]);
                String hostPort = tokens[1];
                log.info("Saved data for " + child + " with shard: " + shard + " and hostPort " + hostPort);
                shardsServers.get(shard).add(hostPort);
                nodesData.put(child, serverData);
            } else {
                log.trace("There is no data for one of the created nodes!");
            }
        }

        isLeader = serverId == leaderId;
        if (isLeader)
            log.info("I'm the servers leader");
        liveServers = children.size();
    }

    String shardPath() {
        return prop.rootPath() + "/" + shard;
    }

    String shardServersPath() {
        return shardPath() + "/servers";
    }

    String serversPath() {
        return prop.rootPath() + "/servers";
    }

    String transactionsPath() {
        return shardPath() + "/transactions";
    }

    String serversReadyPath() {
        return prop.rootPath() + "/ready";
    }

    String shardReportRequestPath() {
        return shardPath() + "/b";
    }


    static String toString(VoteRequest voteRequest) {
        return "voter id:" + voteRequest.getVoterID() + ", to nominee: " + voteRequest.getNominee();
    }
}