package rest;
import com.fasterxml.jackson.annotation.JsonProperty;
// import lombok.Data;
import protos.VotingServerOuterClass.VoteRequest;

public class ClientVoteRequest {
    private int FromServer;
    private int Nominee;
    private int VoterID;

    public ClientVoteRequest(@JsonProperty(value = "FromServer", required = true) int FromServer,
                             @JsonProperty(value = "Nominee", required = true) int Nominee,
                             @JsonProperty(value = "VoterID", required = true) int VoterID) {
        this.FromServer = FromServer;
        this.Nominee = Nominee;
        this.VoterID = VoterID;
    }

    public VoteRequest convertToVoteRequest() {
        return VoteRequest.newBuilder()
                .setFromServer(FromServer)
                .setNominee(Nominee)
                .setVoterID(VoterID)
                .build();
    }
}