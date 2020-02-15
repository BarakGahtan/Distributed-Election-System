package rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import protos.VotingServerOuterClass;

public class ConvertToStateID {
    private int nominee;

    public ConvertToStateID(@JsonProperty(value = "nominee", required = true) int nominee) {
        this.nominee = nominee;
    }

    public VotingServerOuterClass.StateID ConvertToStateID() {
        return VotingServerOuterClass.StateID.newBuilder()
                .setNominee(nominee)
                .build();
    }
}