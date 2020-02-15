package rest;

import java.io.IOException;
import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import state.FrontendWrapper;
import state.Frontend;

@SpringBootApplication
public class RestServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        String restPort = args[0];
        String grpcHostPort = args[1];
        String zookeeperAddress = args[2];
        int shardCount = Integer.parseInt(args[3]);
        int serversPerShard = Integer.parseInt(args[4]);
        int shardNum = Integer.parseInt(args[5]);

        zk.SystemProperties prop = new zk.SystemProperties(zookeeperAddress, shardCount, serversPerShard);
        FrontendWrapper.frontend = new Frontend(prop, shardNum, grpcHostPort);
        FrontendWrapper.frontend.init();

        SpringApplication app = new SpringApplication(RestServer.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", restPort));
        app.run();

        FrontendWrapper.frontend.run();
    }
}