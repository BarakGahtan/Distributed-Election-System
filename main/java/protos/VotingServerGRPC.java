package protos;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 *
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.13.1)",
        comments = "Source: VotingServer.proto")
public final class VotingServerGRPC {

    private VotingServerGRPC() {}

    public static final String SERVICE_NAME = "protos.VotingServer";

    private static abstract class VotingServerBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
        VotingServerBaseDescriptorSupplier() {
        }

        @Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return VotingServerOuterClass.getDescriptor();
        }

        @Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("VotingServer");
        }
    }

    private static final class VotingServerFileDescriptorSupplier
            extends VotingServerBaseDescriptorSupplier {
        VotingServerFileDescriptorSupplier() {
        }
    }

    private static final class VotingServerMethodDescriptorSupplier
            extends VotingServerBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
        private final String methodName;

        VotingServerMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<VotingServerOuterClass.VoteRequest,
            VotingServerOuterClass.STATUS> getTransferVoteRequestMethod;

    private static volatile io.grpc.MethodDescriptor<VotingServerOuterClass.ElectionPeriod,
            VotingServerOuterClass.STATUS> getChangeStatusOfElectiontMethod;

    private static volatile io.grpc.MethodDescriptor<VotingServerOuterClass.StateID,
            VotingServerOuterClass.ReportForState> getReportPerStateMethod;

    public static io.grpc.MethodDescriptor<VotingServerOuterClass.VoteRequest,
            VotingServerOuterClass.STATUS> getTransferVoteRequestMethod() {
        io.grpc.MethodDescriptor<VotingServerOuterClass.VoteRequest, VotingServerOuterClass.STATUS> getTransferVoteRequestMethod;
        if ((getTransferVoteRequestMethod = VotingServerGRPC.getTransferVoteRequestMethod) == null) {
            synchronized (VotingServerGRPC.class) {
                if ((getTransferVoteRequestMethod = VotingServerGRPC.getTransferVoteRequestMethod) == null) {
                    VotingServerGRPC.getTransferVoteRequestMethod = getTransferVoteRequestMethod =
                            io.grpc.MethodDescriptor.<VotingServerOuterClass.VoteRequest, VotingServerOuterClass.STATUS>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "protos.VotingServer", "TransferVoteRequest"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            VotingServerOuterClass.VoteRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            VotingServerOuterClass.STATUS.getDefaultInstance()))
                                    .setSchemaDescriptor(new VotingServerMethodDescriptorSupplier("TransferVoteRequest"))
                                    .build();
                }
            }
        }
        return getTransferVoteRequestMethod;
    }

    public static io.grpc.MethodDescriptor<VotingServerOuterClass.ElectionPeriod,
            VotingServerOuterClass.STATUS> getChangeStatusOfElectionMethod() {
        io.grpc.MethodDescriptor<VotingServerOuterClass.ElectionPeriod, VotingServerOuterClass.STATUS> getChangeStatusOfElectionMethod;
        if ((getChangeStatusOfElectiontMethod = VotingServerGRPC.getChangeStatusOfElectiontMethod) == null) {
            synchronized (VotingServerGRPC.class) {
                if ((getChangeStatusOfElectiontMethod = VotingServerGRPC.getChangeStatusOfElectiontMethod) == null) {
                    VotingServerGRPC.getChangeStatusOfElectiontMethod = getChangeStatusOfElectiontMethod =
                            io.grpc.MethodDescriptor.<VotingServerOuterClass.ElectionPeriod, VotingServerOuterClass.STATUS>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "protos.VotingServer", "ChangeStatusOfElection"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            VotingServerOuterClass.ElectionPeriod.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            VotingServerOuterClass.STATUS.getDefaultInstance()))
                                    .setSchemaDescriptor(new VotingServerMethodDescriptorSupplier("ChangeStatusOfElection"))
                                    .build();
                }
            }
        }
        return getChangeStatusOfElectiontMethod;
    }

    // Static method descriptors that strictly reflect the proto.
    public static io.grpc.MethodDescriptor<VotingServerOuterClass.StateID,
            VotingServerOuterClass.ReportForState> getReportPerStateMethod() {
        io.grpc.MethodDescriptor<VotingServerOuterClass.StateID, VotingServerOuterClass.ReportForState> getReportPerStateMethod;
        if ((getReportPerStateMethod = VotingServerGRPC.getReportPerStateMethod) == null) {
            synchronized (VotingServerGRPC.class) {
                if ((getReportPerStateMethod = VotingServerGRPC.getReportPerStateMethod) == null) {
                    VotingServerGRPC.getReportPerStateMethod = getReportPerStateMethod =
                            io.grpc.MethodDescriptor.<VotingServerOuterClass.StateID, VotingServerOuterClass.ReportForState>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(
                                            "protos.VotingServer", "ReportPerState"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            VotingServerOuterClass.StateID.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            VotingServerOuterClass.ReportForState.getDefaultInstance()))
                                    .setSchemaDescriptor(new VotingServerMethodDescriptorSupplier("ReportPerState"))
                                    .build();
                }
            }
        }
        return getReportPerStateMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static VotingServerStub newStub(io.grpc.Channel channel) {
        return new VotingServerStub(channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static VotingServerBlockingStub newBlockingStub(
            io.grpc.Channel channel) {
        return new VotingServerBlockingStub(channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static VotingServerFutureStub newFutureStub(
            io.grpc.Channel channel) {
        return new VotingServerFutureStub(channel);
    }

    /**
     *
     */
    public static abstract class VotingServerImplBase implements io.grpc.BindableService {
        /**
         *
         */
        public void TransferVoteRequest(VotingServerOuterClass.VoteRequest request,
                                        io.grpc.stub.StreamObserver<VotingServerOuterClass.STATUS> responseObserver) {
            asyncUnimplementedUnaryCall(getTransferVoteRequestMethod(), responseObserver);
        }

        public void ReportPerState(VotingServerOuterClass.StateID request,
                                   io.grpc.stub.StreamObserver<VotingServerOuterClass.ReportForState> responseObserver) {
            asyncUnimplementedUnaryCall(getReportPerStateMethod(), responseObserver);
        }
        public void ChangeStatusOfElection(VotingServerOuterClass.ElectionPeriod request,
                                           io.grpc.stub.StreamObserver<VotingServerOuterClass.STATUS> responseObserver) {
            asyncUnimplementedUnaryCall(getChangeStatusOfElectionMethod(), responseObserver);
        }
        @Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(
                            getTransferVoteRequestMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            VotingServerOuterClass.VoteRequest,
                                            VotingServerOuterClass.STATUS>(
                                            this, METHODID_TRANSFER_VOTE_REQUEST)))
                    .addMethod(
                            getChangeStatusOfElectionMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            VotingServerOuterClass.ElectionPeriod,
                                            VotingServerOuterClass.STATUS>(
                                            this, METHODID_CHANGE_STATUS_OF_ELECTION)))
                    .addMethod(
                            getReportPerStateMethod(),
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            VotingServerOuterClass.StateID,
                                            VotingServerOuterClass.ReportForState>(
                                            this, METHODID_REPORT_PER_STATE)))
                    .build();
        }
    }

    /**
     *
     */
    public static final class VotingServerStub extends io.grpc.stub.AbstractStub<VotingServerStub> {
        private VotingServerStub(io.grpc.Channel channel) {
            super(channel);
        }

        private VotingServerStub(io.grpc.Channel channel,
                                 io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected VotingServerStub build(io.grpc.Channel channel,
                                         io.grpc.CallOptions callOptions) {
            return new VotingServerStub(channel, callOptions);
        }

        /**
         *
         */
        public void TransferVoteRequest(VotingServerOuterClass.VoteRequest request,
                                        io.grpc.stub.StreamObserver<VotingServerOuterClass.STATUS> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getTransferVoteRequestMethod(), getCallOptions()), request, responseObserver);
        }

        public void ChangeStatusOfElection(VotingServerOuterClass.ElectionPeriod request,
                                           io.grpc.stub.StreamObserver<VotingServerOuterClass.STATUS> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getChangeStatusOfElectionMethod(), getCallOptions()), request, responseObserver);
        }

        public void ReportPerState(VotingServerOuterClass.StateID request,
                                   io.grpc.stub.StreamObserver<VotingServerOuterClass.ReportForState> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(getReportPerStateMethod(), getCallOptions()), request, responseObserver);
        }

    }

    /**
     *
     */
    public static final class VotingServerBlockingStub extends io.grpc.stub.AbstractStub<VotingServerBlockingStub> {
        private VotingServerBlockingStub(io.grpc.Channel channel) {
            super(channel);
        }

        private VotingServerBlockingStub(io.grpc.Channel channel,
                                         io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected VotingServerBlockingStub build(io.grpc.Channel channel,
                                                 io.grpc.CallOptions callOptions) {
            return new VotingServerBlockingStub(channel, callOptions);
        }

        /**
         *
         */
        public VotingServerOuterClass.STATUS TransferVoteRequest(VotingServerOuterClass.VoteRequest request) {
            return blockingUnaryCall(
                    getChannel(), getTransferVoteRequestMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public VotingServerOuterClass.STATUS ChangeStatusOfElection(VotingServerOuterClass.ElectionPeriod request) {
            return blockingUnaryCall(
                    getChannel(), getChangeStatusOfElectionMethod(), getCallOptions(), request);
        }
        /**
         *
         */
        public VotingServerOuterClass.ReportForState ReportPerState(VotingServerOuterClass.StateID request) {
            return blockingUnaryCall(
                    getChannel(), getReportPerStateMethod(), getCallOptions(), request);
        }
    }

    /**
     *
     */
    public static final class VotingServerFutureStub extends io.grpc.stub.AbstractStub<VotingServerFutureStub> {
        private VotingServerFutureStub(io.grpc.Channel channel) {
            super(channel);
        }

        private VotingServerFutureStub(io.grpc.Channel channel,
                                       io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected VotingServerFutureStub build(io.grpc.Channel channel,
                                               io.grpc.CallOptions callOptions) {
            return new VotingServerFutureStub(channel, callOptions);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<VotingServerOuterClass.STATUS> TransferVoteRequest(
                VotingServerOuterClass.VoteRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getTransferVoteRequestMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<VotingServerOuterClass.STATUS> ChangeStatusOfElection(
                VotingServerOuterClass.ElectionPeriod request) {
            return futureUnaryCall(
                    getChannel().newCall(getChangeStatusOfElectionMethod(), getCallOptions()), request);
        }
        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<VotingServerOuterClass.ReportForState> ReportPerState(
                VotingServerOuterClass.StateID request) {
            return futureUnaryCall(
                    getChannel().newCall(getReportPerStateMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_TRANSFER_VOTE_REQUEST = 0;
    private static final int METHODID_CHANGE_STATUS_OF_ELECTION = 1;
    private static final int METHODID_REPORT_PER_STATE = 2;
    private static final class MethodHandlers<Req, Resp> implements
            io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
        private final VotingServerImplBase serviceImpl;
        private final int methodId;

        MethodHandlers(VotingServerImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_TRANSFER_VOTE_REQUEST:
                    serviceImpl.TransferVoteRequest((VotingServerOuterClass.VoteRequest) request,
                            (io.grpc.stub.StreamObserver<VotingServerOuterClass.STATUS>) responseObserver);
                    break;
                case METHODID_CHANGE_STATUS_OF_ELECTION:
                    serviceImpl.ChangeStatusOfElection((VotingServerOuterClass.ElectionPeriod) request,
                            (io.grpc.stub.StreamObserver<VotingServerOuterClass.STATUS>) responseObserver);
                    break;
                case METHODID_REPORT_PER_STATE:
                    serviceImpl.ReportPerState((VotingServerOuterClass.StateID) request,
                            (io.grpc.stub.StreamObserver<VotingServerOuterClass.ReportForState>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(
                io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }
    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (VotingServerGRPC.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                            .setSchemaDescriptor(new VotingServerFileDescriptorSupplier())
                            .addMethod(getTransferVoteRequestMethod())
                            .addMethod(getChangeStatusOfElectionMethod())
                            .addMethod(getReportPerStateMethod())
                            .build();
                }
            }
        }
        return result;
    }
}

