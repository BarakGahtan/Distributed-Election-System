package rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import protos.VotingServerOuterClass;
import state.Connection;
import state.Frontend;
import state.FrontendWrapper;

@RestController
@Component
public class RestServerController {
    private FrontendWrapper frontendWrapper;

    @Autowired
    RestServerController(FrontendWrapper frontendWrapper) {
        this.frontendWrapper = frontendWrapper;
    }

    @RequestMapping(value = "/{nominee}/{voterId}", method = RequestMethod.PUT)
    public String vote(@PathVariable int nominee, @PathVariable int voterId) {
        String result = "Got vote request for {nominee, " + nominee + "}, {voterId, " + voterId + "}\n";
        result = result + FrontendWrapper.frontend.vote(nominee, voterId);
        return result;
    }

    @RequestMapping(value = "/report", method = RequestMethod.PUT)
    public String requestReport() {
        if (!FrontendWrapper.frontend.committee)
            return "Bad Request";

        String result;
        Integer winner = FrontendWrapper.frontend.winningNominee();
        if (winner == -2)
            result = "Elections has not started yet!";
        else if (winner == -1)
            result = "Its a tie!";
        else
            result = "The winning candidate is " + winner.toString();

        System.out.println(result);
        return result;
    }

    @RequestMapping(value = "/report/{shard}", method = RequestMethod.PUT)
    public String requestReportShard(@PathVariable int shard) {
        if (!FrontendWrapper.frontend.committee)
            return "Bad Request";

        Integer winner = FrontendWrapper.frontend.committeeShardReport(shard);
        if (winner == -2)
            return "Elections has already started!";
        return "The winning candidate in shard " + shard + " is " + winner.toString();
    }

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public String startElections() {
        if (!FrontendWrapper.frontend.committee)
            return "Bad Request";

        int r = FrontendWrapper.frontend.committeeStartElections("voterDb.txt");
        if (r == -2)
            return "Elections has already started!";
        return "Sent start election to a random server";
    }

    @RequestMapping(value = "/end", method = RequestMethod.POST)
    public String endElections() {
        if (!FrontendWrapper.frontend.committee)
            return "Bad Request";

        int r = FrontendWrapper.frontend.committeeEndElections();
        if (r != 0)
            return "Elections are not yet running!";

        return "Sent end message";
    }
}
