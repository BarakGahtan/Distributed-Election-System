package rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import protos.VotingServerOuterClass;

public class ConvertToReportForState {
    private int nominee;

    public ConvertToReportForState(@JsonProperty(value = "nominee", required = true) int nominee) {
        this.nominee = nominee;
    }

    public VotingServerOuterClass.StateID ConvertToReportForState() {
        return VotingServerOuterClass.StateID.newBuilder()
                .setNominee(nominee)
                .build();
    }
}