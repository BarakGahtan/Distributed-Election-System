package zk;

public class SystemProperties {
    public static  final String ElectionStartString = "Start";
    public static  final String ElectionEndString = "End";

    public static final String root = "election";
    public static final String serverBaseName = "server";
    public static final String transactionBaseName = "voteTransaction";
    public String zookeeperAddress;
    public int shardCount;
    public int serversPerShard;
    public int nomineeCount;

    public SystemProperties(String zookeeperAddress, int shardCount, int serversPerShard) {
        this.zookeeperAddress = zookeeperAddress;
        this.shardCount = shardCount;
        this.serversPerShard = serversPerShard;
    }

    public int serverCount() {
        return shardCount * serversPerShard;
    }

    public int metaZnodeCount() {
        return 1 + shardCount;
    }

    public String rootPath() {
        return "/" + root;
    }

    public int finalZnodeCount() {
        return 1 + shardCount * (1 + serversPerShard);
    }
};