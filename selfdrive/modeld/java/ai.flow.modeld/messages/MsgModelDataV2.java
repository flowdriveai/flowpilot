package ai.flow.modeld.messages;

import ai.flow.definitions.Definitions;
import ai.flow.definitions.MessageBase;
import ai.flow.modeld.ParsedOutputs;
import ai.flow.modeld.CommonModel;
import org.capnproto.PrimitiveList;
import org.capnproto.StructList;

import java.nio.ByteBuffer;

public class MsgModelDataV2 extends MessageBase {

    public Definitions.ModelDataV2.Builder modelDataV2;

    public Definitions.ModelDataV2.XYZTData.Builder position;
    public Definitions.ModelDataV2.XYZTData.Builder velocity;
    public Definitions.ModelDataV2.XYZTData.Builder orientation;
    public Definitions.ModelDataV2.XYZTData.Builder orientationRate;
    public Definitions.ModelDataV2.XYZTData.Builder acceleration;

    public PrimitiveList.Float.Builder positionX;
    public PrimitiveList.Float.Builder positionY;
    public PrimitiveList.Float.Builder positionZ;
    public PrimitiveList.Float.Builder positionT;

    public PrimitiveList.Float.Builder velocityX;
    public PrimitiveList.Float.Builder velocityY;
    public PrimitiveList.Float.Builder velocityZ;
    public PrimitiveList.Float.Builder velocityT;

    public PrimitiveList.Float.Builder orientationX;
    public PrimitiveList.Float.Builder orientationY;
    public PrimitiveList.Float.Builder orientationZ;
    public PrimitiveList.Float.Builder orientationT;

    public PrimitiveList.Float.Builder orientationRateX;
    public PrimitiveList.Float.Builder orientationRateY;
    public PrimitiveList.Float.Builder orientationRateZ;
    public PrimitiveList.Float.Builder orientationRateT;

    public PrimitiveList.Float.Builder accelerationX;
    public PrimitiveList.Float.Builder accelerationY;
    public PrimitiveList.Float.Builder accelerationZ;
    public PrimitiveList.Float.Builder accelerationT;

    public StructList.Builder<Definitions.ModelDataV2.XYZTData.Builder> laneLines;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine1;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine2;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine3;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine4;

    public PrimitiveList.Float.Builder laneLineX1;
    public PrimitiveList.Float.Builder laneLineY1;
    public PrimitiveList.Float.Builder laneLineZ1;
    public PrimitiveList.Float.Builder laneLineT1;
    public PrimitiveList.Float.Builder laneLineStds1;

    public PrimitiveList.Float.Builder laneLineX2;
    public PrimitiveList.Float.Builder laneLineY2;
    public PrimitiveList.Float.Builder laneLineZ2;
    public PrimitiveList.Float.Builder laneLineT2;
    public PrimitiveList.Float.Builder laneLine2Stds;

    public PrimitiveList.Float.Builder laneLineX3;
    public PrimitiveList.Float.Builder laneLineY3;
    public PrimitiveList.Float.Builder laneLineZ3;
    public PrimitiveList.Float.Builder laneLineT3;
    public PrimitiveList.Float.Builder laneLine3Stds;

    public PrimitiveList.Float.Builder laneLineX4;
    public PrimitiveList.Float.Builder laneLineY4;
    public PrimitiveList.Float.Builder laneLineZ4;
    public PrimitiveList.Float.Builder laneLineT4;
    public PrimitiveList.Float.Builder laneLine4Stds;

    public PrimitiveList.Float.Builder laneLineProbs;

    public StructList.Builder<Definitions.ModelDataV2.XYZTData.Builder> roadEdges;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge1;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge2;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge3;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge4;

    public PrimitiveList.Float.Builder roadEdgeX1;
    public PrimitiveList.Float.Builder roadEdgeY1;
    public PrimitiveList.Float.Builder roadEdgeZ1;
    public PrimitiveList.Float.Builder roadEdgeT1;
    public PrimitiveList.Float.Builder roadEdge1Stds;

    public PrimitiveList.Float.Builder roadEdgeX2;
    public PrimitiveList.Float.Builder roadEdgeY2;
    public PrimitiveList.Float.Builder roadEdgeZ2;
    public PrimitiveList.Float.Builder roadEdgeT2;
    public PrimitiveList.Float.Builder roadEdge2Stds;

    public PrimitiveList.Float.Builder roadEdgeX3;
    public PrimitiveList.Float.Builder roadEdgeY3;
    public PrimitiveList.Float.Builder roadEdgeZ3;
    public PrimitiveList.Float.Builder roadEdgeT3;
    public PrimitiveList.Float.Builder roadEdge3Stds;

    public PrimitiveList.Float.Builder roadEdgeX4;
    public PrimitiveList.Float.Builder roadEdgeY4;
    public PrimitiveList.Float.Builder roadEdgeZ4;
    public PrimitiveList.Float.Builder roadEdgeT4;
    public PrimitiveList.Float.Builder roadEdge4Stds;

    public StructList.Builder<Definitions.ModelDataV2.LeadDataV2.Builder> leads;
    public Definitions.ModelDataV2.LeadDataV2.Builder leads1;
    public Definitions.ModelDataV2.LeadDataV2.Builder leads2;
    public Definitions.ModelDataV2.LeadDataV2.Builder leads3;

    public PrimitiveList.Float.Builder xyva1;
    public PrimitiveList.Float.Builder xyvaStd1;

    public PrimitiveList.Float.Builder xyva2;
    public PrimitiveList.Float.Builder xyvaStd2 ;

    public PrimitiveList.Float.Builder xyva3;
    public PrimitiveList.Float.Builder xyvaStd3;

    public Definitions.ModelDataV2.MetaData.Builder meta;
    public Definitions.ModelDataV2.DisengagePredictions.Builder disengagePredictions;
    public PrimitiveList.Float.Builder desireState;
    public PrimitiveList.Float.Builder desirePredictions;
    public PrimitiveList.Float.Builder gasDesengageProbs; // SIZE NOT DETERMINED
    public PrimitiveList.Float.Builder t; // SIZE NOT DETERMINED
    public PrimitiveList.Float.Builder brakeDisengageProbs; // SIZE NOT DETERMINED
    public PrimitiveList.Float.Builder steerOverrideProbs; // SIZE NOT DETERMINED
    public PrimitiveList.Float.Builder brake3MetersPerSecondSquaredProbs; // SIZE NOT DETERMINED
    public PrimitiveList.Float.Builder brake4MetersPerSecondSquaredProbs; // SIZE NOT DETERMINED
    public PrimitiveList.Float.Builder brake5MetersPerSecondSquaredProbs; // SIZE NOT DETERMINED

    public MsgModelDataV2(ByteBuffer rawMessageBuffer) {
        super(rawMessageBuffer);
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    public MsgModelDataV2() {
        super();
        initFields();
        bytesSerializedForm = computeSerializedMsgBytes();
        initSerializedBuffer();
    }

    private void initFields(){
        event = messageBuilder.initRoot(Definitions.Event.factory);
        modelDataV2 = event.initModelV2();

        position = modelDataV2.initPosition();
        velocity = modelDataV2.initVelocity();
        orientation = modelDataV2.initOrientation();
        orientationRate = modelDataV2.initOrientationRate();
        acceleration = modelDataV2.initAcceleration();

        positionX = position.initX(CommonModel.TRAJECTORY_SIZE);
        positionY = position.initY(CommonModel.TRAJECTORY_SIZE);
        positionZ = position.initZ(CommonModel.TRAJECTORY_SIZE);
        positionT = position.initT(CommonModel.TRAJECTORY_SIZE);
        velocityX = velocity.initX(CommonModel.TRAJECTORY_SIZE);
        velocityY = velocity.initY(CommonModel.TRAJECTORY_SIZE);
        velocityZ = velocity.initZ(CommonModel.TRAJECTORY_SIZE);
        velocityT = velocity.initT(CommonModel.TRAJECTORY_SIZE);
        orientationX = orientation.initX(CommonModel.TRAJECTORY_SIZE);
        orientationY = orientation.initY(CommonModel.TRAJECTORY_SIZE);
        orientationZ = orientation.initZ(CommonModel.TRAJECTORY_SIZE);
        orientationT = orientation.initT(CommonModel.TRAJECTORY_SIZE);
        orientationRateX = orientationRate.initX(CommonModel.TRAJECTORY_SIZE);
        orientationRateY = orientationRate.initY(CommonModel.TRAJECTORY_SIZE);
        orientationRateZ = orientationRate.initZ(CommonModel.TRAJECTORY_SIZE);
        orientationRateT = orientationRate.initT(CommonModel.TRAJECTORY_SIZE);
        accelerationX = acceleration.initX(CommonModel.TRAJECTORY_SIZE);
        accelerationY = acceleration.initY(CommonModel.TRAJECTORY_SIZE);
        accelerationZ = acceleration.initZ(CommonModel.TRAJECTORY_SIZE);
        accelerationT = acceleration.initT(CommonModel.TRAJECTORY_SIZE);

        laneLines = modelDataV2.initLaneLines(4);
        laneLine1 = laneLines.get(0);
        laneLine2 = laneLines.get(1);
        laneLine3 = laneLines.get(2);
        laneLine4 = laneLines.get(3);
        laneLineX1 = laneLine1.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY1 = laneLine1.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ1 = laneLine1.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT1 = laneLine1.initT(CommonModel.TRAJECTORY_SIZE);
        laneLineStds1 = modelDataV2.initLaneLineStds(CommonModel.TRAJECTORY_SIZE * 2);
        laneLineX2 = laneLine2.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY2 = laneLine2.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ2 = laneLine2.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT2 = laneLine2.initT(CommonModel.TRAJECTORY_SIZE);
        laneLine2Stds = modelDataV2.initLaneLineStds(CommonModel.TRAJECTORY_SIZE * 2);
        laneLineX3 = laneLine3.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY3 = laneLine3.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ3 = laneLine3.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT3 = laneLine3.initT(CommonModel.TRAJECTORY_SIZE);
        laneLine3Stds = modelDataV2.initLaneLineStds(CommonModel.TRAJECTORY_SIZE * 2);
        laneLineX4 = laneLine4.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY4 = laneLine4.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ4 = laneLine4.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT4 = laneLine4.initT(CommonModel.TRAJECTORY_SIZE);
        laneLine4Stds = modelDataV2.initLaneLineStds(CommonModel.TRAJECTORY_SIZE * 2);
        laneLineProbs = modelDataV2.initLaneLineProbs(4);
        roadEdges = modelDataV2.initRoadEdges(4);
        roadEdge1 = roadEdges.get(0);
        roadEdge2 = roadEdges.get(1);
        roadEdge3 = roadEdges.get(2);
        roadEdge4 = roadEdges.get(3);

        roadEdgeX1 = roadEdge1.initX(CommonModel.TRAJECTORY_SIZE);
        roadEdgeY1 = roadEdge1.initY(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZ1 = roadEdge1.initZ(CommonModel.TRAJECTORY_SIZE);
        roadEdgeT1 = roadEdge1.initT(CommonModel.TRAJECTORY_SIZE);
        roadEdge1Stds = modelDataV2.initRoadEdgeStds(CommonModel.TRAJECTORY_SIZE * 2);
        roadEdgeX2 = roadEdge2.initX(CommonModel.TRAJECTORY_SIZE);
        roadEdgeY2 = roadEdge2.initY(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZ2 = roadEdge2.initZ(CommonModel.TRAJECTORY_SIZE);
        roadEdgeT2 = roadEdge2.initT(CommonModel.TRAJECTORY_SIZE);
        roadEdge2Stds = modelDataV2.initRoadEdgeStds(CommonModel.TRAJECTORY_SIZE * 2);
        roadEdgeX3 = roadEdge3.initX(CommonModel.TRAJECTORY_SIZE);
        roadEdgeY3 = roadEdge3.initY(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZ3 = roadEdge3.initZ(CommonModel.TRAJECTORY_SIZE);
        roadEdgeT3 = roadEdge3.initT(CommonModel.TRAJECTORY_SIZE);
        roadEdge3Stds = modelDataV2.initRoadEdgeStds(CommonModel.TRAJECTORY_SIZE * 2);
        roadEdgeX4 = roadEdge4.initX(CommonModel.TRAJECTORY_SIZE);
        roadEdgeY4 = roadEdge4.initY(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZ4 = roadEdge4.initZ(CommonModel.TRAJECTORY_SIZE);
        roadEdgeT4 = roadEdge4.initT(CommonModel.TRAJECTORY_SIZE);
        roadEdge4Stds = modelDataV2.initRoadEdgeStds(CommonModel.TRAJECTORY_SIZE * 2);

        leads = modelDataV2.initLeads(3);
        leads1 = leads.get(0);
        leads2 = leads.get(1);
        leads3 = leads.get(2);

        xyva1 = leads1.initXyva(4);
        xyvaStd1 = leads1.initXyvaStd(4);
        xyva2 = leads2.initXyva(4);
        xyvaStd2 = leads2.initXyvaStd(4);
        xyva3 = leads3.initXyva(4);
        xyvaStd3 = leads3.initXyvaStd(4);

        meta = modelDataV2.initMeta();
        disengagePredictions = meta.initDisengagePredictions();

        desireState = meta.initDesireState(512);
        desirePredictions = meta.initDesirePrediction(512);
        gasDesengageProbs = disengagePredictions.initGasDisengageProbs(44); // SIZE NOT DETERMINED
        t = disengagePredictions.initT(44); // SIZE NOT DETERMINED
        brakeDisengageProbs = disengagePredictions.initBrakeDisengageProbs(44); // SIZE NOT DETERMINED
        steerOverrideProbs = disengagePredictions.initSteerOverrideProbs(44); // SIZE NOT DETERMINED
        brake3MetersPerSecondSquaredProbs = disengagePredictions.initBrake3MetersPerSecondSquaredProbs(44); // SIZE NOT DETERMINED
        brake4MetersPerSecondSquaredProbs = disengagePredictions.initBrake4MetersPerSecondSquaredProbs(44); // SIZE NOT DETERMINED
        brake5MetersPerSecondSquaredProbs = disengagePredictions.initBrake5MetersPerSecondSquaredProbs(44); // SIZE NOT DETERMINED
    }

    public static void fillParsed(ParsedOutputs parsed, Definitions.ModelDataV2.Reader msg, boolean full) { // TODO Avoid this
        System.out.println("here");
        for (int i = 0; i < CommonModel.TRAJECTORY_SIZE; i++) {
            parsed.position.get(0)[i] = msg.getPosition().getX().get(i);
            parsed.position.get(1)[i] = msg.getPosition().getY().get(i);
            parsed.position.get(2)[i] = msg.getPosition().getZ().get(i);
            parsed.position.get(3)[i] = msg.getPosition().getT().get(i);

            parsed.velocity.get(0)[i] = msg.getVelocity().getX().get(i);
            parsed.velocity.get(1)[i] = msg.getVelocity().getY().get(i);
            parsed.velocity.get(2)[i] = msg.getVelocity().getZ().get(i);
            parsed.velocity.get(3)[i] = msg.getVelocity().getT().get(i);

            parsed.position.get(0)[i] = msg.getPosition().getX().get(i);
            parsed.position.get(1)[i] = msg.getPosition().getY().get(i);
            parsed.position.get(2)[i] = msg.getPosition().getZ().get(i);
            parsed.position.get(3)[i] = msg.getPosition().getT().get(i);

            parsed.position.get(0)[i] = msg.getPosition().getX().get(i);
            parsed.position.get(1)[i] = msg.getPosition().getY().get(i);
            parsed.position.get(2)[i] = msg.getPosition().getZ().get(i);
            parsed.position.get(3)[i] = msg.getPosition().getT().get(i);

            if (full) {
                parsed.laneLines.get(0).get(0)[i] = msg.getLaneLines().get(0).getX().get(i);
                parsed.laneLines.get(0).get(1)[i] = msg.getLaneLines().get(0).getY().get(i);
                parsed.laneLines.get(0).get(2)[i] = msg.getLaneLines().get(0).getZ().get(i);
                parsed.laneLines.get(0).get(3)[i] = msg.getLaneLines().get(0).getT().get(i);

                parsed.laneLines.get(1).get(0)[i] = msg.getLaneLines().get(1).getX().get(i);
                parsed.laneLines.get(1).get(1)[i] = msg.getLaneLines().get(1).getY().get(i);
                parsed.laneLines.get(1).get(2)[i] = msg.getLaneLines().get(1).getZ().get(i);
                parsed.laneLines.get(1).get(3)[i] = msg.getLaneLines().get(1).getT().get(i);

                parsed.laneLines.get(2).get(0)[i] = msg.getLaneLines().get(2).getX().get(i);
                parsed.laneLines.get(2).get(1)[i] = msg.getLaneLines().get(2).getY().get(i);
                parsed.laneLines.get(2).get(2)[i] = msg.getLaneLines().get(2).getZ().get(i);
                parsed.laneLines.get(2).get(3)[i] = msg.getLaneLines().get(2).getT().get(i);

                parsed.laneLines.get(3).get(0)[i] = msg.getLaneLines().get(3).getX().get(i);
                parsed.laneLines.get(3).get(1)[i] = msg.getLaneLines().get(3).getY().get(i);
                parsed.laneLines.get(3).get(2)[i] = msg.getLaneLines().get(3).getZ().get(i);
                parsed.laneLines.get(3).get(3)[i] = msg.getLaneLines().get(3).getT().get(i);

                parsed.roadEdges.get(0).get(0)[i] = msg.getRoadEdges().get(0).getX().get(i);
                parsed.roadEdges.get(0).get(1)[i] = msg.getRoadEdges().get(0).getY().get(i);
                parsed.roadEdges.get(0).get(2)[i] = msg.getRoadEdges().get(0).getZ().get(i);
                parsed.roadEdges.get(0).get(3)[i] = msg.getRoadEdges().get(0).getT().get(i);

                parsed.roadEdges.get(1).get(0)[i] = msg.getRoadEdges().get(1).getX().get(i);
                parsed.roadEdges.get(1).get(1)[i] = msg.getRoadEdges().get(1).getY().get(i);
                parsed.roadEdges.get(1).get(2)[i] = msg.getRoadEdges().get(1).getZ().get(i);
                parsed.roadEdges.get(1).get(3)[i] = msg.getRoadEdges().get(1).getT().get(i);
            }
        }

        for (int i = 0; i < 4; i++) {
            parsed.leads.get(0).xyva[i] = msg.getLeads().get(0).getXyva().get(i);
            parsed.leads.get(1).xyva[i] = msg.getLeads().get(1).getXyva().get(i);
            parsed.leads.get(2).xyva[i] = msg.getLeads().get(2).getXyva().get(i);

            parsed.leads.get(0).xyvaStd[i] = msg.getLeads().get(0).getXyvaStd().get(i);
            parsed.leads.get(1).xyvaStd[i] = msg.getLeads().get(1).getXyvaStd().get(i);
            parsed.leads.get(2).xyvaStd[i] = msg.getLeads().get(2).getXyvaStd().get(i);
        }

        parsed.leads.get(0).prob = msg.getLeads().get(0).getProb();
        System.out.println(parsed.leads.get(0).prob);
        parsed.leads.get(1).prob = msg.getLeads().get(1).getProb();
        parsed.leads.get(2).prob = msg.getLeads().get(2).getProb();



        if (full) {

            for (int i = 0; i < CommonModel.TRAJECTORY_SIZE * 2; i++) {
                parsed.laneLineStds.get(0)[i] = msg.getLaneLineStds().get(i);
                parsed.laneLineStds.get(1)[i] = msg.getLaneLineStds().get(i);
                parsed.laneLineStds.get(2)[i] = msg.getLaneLineStds().get(i);
                parsed.laneLineStds.get(3)[i] = msg.getLaneLineStds().get(i);

                parsed.roadEdgeStds.get(0)[i] = msg.getRoadEdgeStds().get(i);
                parsed.roadEdgeStds.get(1)[i] = msg.getRoadEdgeStds().get(i);
                parsed.roadEdgeStds.get(2)[i] = msg.getRoadEdgeStds().get(i);
                parsed.roadEdgeStds.get(3)[i] = msg.getRoadEdgeStds().get(i);
            }

            parsed.laneLineProbs[0] = msg.getLaneLineProbs().get(0);
            parsed.laneLineProbs[1] = msg.getLaneLineProbs().get(1);
            parsed.laneLineProbs[2] = msg.getLaneLineProbs().get(2);
            parsed.laneLineProbs[3] = msg.getLaneLineProbs().get(3);
        }
    }

    public void fill(ParsedOutputs parsed, long timestamp, int frameId,
                    int frameAge, float frameDropPerc, float modelExecutionTime,
                    float gpuExecutionTime) {

        modelDataV2.setFrameId(frameId);
        modelDataV2.setFrameAge(frameAge);
        modelDataV2.setFrameDropPerc(frameDropPerc);
        modelDataV2.setTimestampEof(timestamp);
        modelDataV2.setGpuExecutionTime(gpuExecutionTime);

        for (int i = 0; i < CommonModel.TRAJECTORY_SIZE; i++) {
            positionX.set(i, parsed.position.get(0)[i]);
            positionY.set(i, parsed.position.get(1)[i]);
            positionZ.set(i, parsed.position.get(2)[i]);
            positionT.set(i, parsed.position.get(3)[i]);

            velocityX.set(i, parsed.velocity.get(0)[i]);
            velocityY.set(i, parsed.velocity.get(1)[i]);
            velocityZ.set(i, parsed.velocity.get(2)[i]);
            velocityT.set(i, parsed.velocity.get(3)[i]);

            orientationX.set(i, parsed.orientation.get(0)[i]);
            orientationY.set(i, parsed.orientation.get(1)[i]);
            orientationZ.set(i, parsed.orientation.get(2)[i]);
            orientationT.set(i, parsed.orientation.get(3)[i]);

            orientationRateX.set(i, parsed.orientationRate.get(0)[i]);
            orientationRateY.set(i, parsed.orientationRate.get(1)[i]);
            orientationRateZ.set(i, parsed.orientationRate.get(2)[i]);
            orientationRateT.set(i, parsed.orientationRate.get(3)[i]);

            accelerationX.set(i, parsed.acceleration.get(0)[i]);
            accelerationY.set(i, parsed.acceleration.get(1)[i]);
            accelerationZ.set(i, parsed.acceleration.get(2)[i]);
            accelerationT.set(i, parsed.acceleration.get(3)[i]);

            laneLineX1.set(i, parsed.laneLines.get(0).get(0)[i]);
            laneLineY1.set(i, parsed.laneLines.get(0).get(1)[i]);
            laneLineZ1.set(i, parsed.laneLines.get(0).get(2)[i]);
            laneLineT1.set(i, parsed.laneLines.get(0).get(3)[i]);

            laneLineX2.set(i, parsed.laneLines.get(1).get(0)[i]);
            laneLineY2.set(i, parsed.laneLines.get(1).get(1)[i]);
            laneLineZ2.set(i, parsed.laneLines.get(1).get(2)[i]);
            laneLineT2.set(i, parsed.laneLines.get(1).get(3)[i]);

            laneLineX3.set(i, parsed.laneLines.get(2).get(0)[i]);
            laneLineY3.set(i, parsed.laneLines.get(2).get(1)[i]);
            laneLineZ3.set(i, parsed.laneLines.get(2).get(2)[i]);
            laneLineT3.set(i, parsed.laneLines.get(2).get(3)[i]);

            laneLineX4.set(i, parsed.laneLines.get(3).get(0)[i]);
            laneLineY4.set(i, parsed.laneLines.get(3).get(1)[i]);
            laneLineZ4.set(i, parsed.laneLines.get(3).get(2)[i]);
            laneLineT4.set(i, parsed.laneLines.get(3).get(3)[i]);

            roadEdgeX1.set(i, parsed.roadEdges.get(0).get(0)[i]);
            roadEdgeY1.set(i, parsed.roadEdges.get(0).get(1)[i]);
            roadEdgeZ1.set(i, parsed.roadEdges.get(0).get(2)[i]);
            roadEdgeT1.set(i, parsed.roadEdges.get(0).get(3)[i]);

            roadEdgeX2.set(i, parsed.roadEdges.get(1).get(0)[i]);
            roadEdgeY2.set(i, parsed.roadEdges.get(1).get(1)[i]);
            roadEdgeZ2.set(i, parsed.roadEdges.get(1).get(2)[i]);
            roadEdgeT2.set(i, parsed.roadEdges.get(1).get(3)[i]);

            roadEdgeX3.set(i, parsed.roadEdges.get(2).get(0)[i]);
            roadEdgeY3.set(i, parsed.roadEdges.get(2).get(1)[i]);
            roadEdgeZ3.set(i, parsed.roadEdges.get(2).get(2)[i]);
            roadEdgeT3.set(i, parsed.roadEdges.get(2).get(3)[i]);

            roadEdgeX4.set(i, parsed.roadEdges.get(3).get(0)[i]);
            roadEdgeY4.set(i, parsed.roadEdges.get(3).get(1)[i]);
            roadEdgeZ4.set(i, parsed.roadEdges.get(3).get(2)[i]);
            roadEdgeT4.set(i, parsed.roadEdges.get(3).get(3)[i]);
        }

        for (int i = 0; i < CommonModel.TRAJECTORY_SIZE * 2; i++) {
            laneLineStds1.set(i, parsed.laneLineStds.get(0)[i]);
            laneLine2Stds.set(i, parsed.laneLineStds.get(1)[i]);
            laneLine3Stds.set(i, parsed.laneLineStds.get(2)[i]);
            laneLine4Stds.set(i, parsed.laneLineStds.get(3)[i]);

            roadEdge1Stds.set(i, parsed.roadEdgeStds.get(0)[i]);
            roadEdge2Stds.set(i, parsed.roadEdgeStds.get(1)[i]);
            roadEdge3Stds.set(i, parsed.roadEdgeStds.get(2)[i]);
            roadEdge4Stds.set(i, parsed.roadEdgeStds.get(3)[i]);
        }

        for (int i = 0; i < 4; i++)
            laneLineProbs.set(i, parsed.laneLineProbs[i]);

        for (int i = 0; i < 4; i++) {
            xyva1.set(i, parsed.leads.get(0).xyva[i]);
            xyva2.set(i, parsed.leads.get(1).xyva[i]);
            xyva3.set(i, parsed.leads.get(2).xyva[i]);

            xyvaStd1.set(i, parsed.leads.get(0).xyvaStd[i]);
            xyvaStd2.set(i, parsed.leads.get(1).xyvaStd[i]);
            xyvaStd3.set(i, parsed.leads.get(2).xyvaStd[i]);
        }

        for (int i = 0; i < CommonModel.DESIRE_LEN; i++)
            desireState.set(i, parsed.metaData.desireState[i]);

        for (int i = 0; i < CommonModel.DESIRE_LEN*4; i++)
            desirePredictions.set(i, parsed.metaData.desirePrediction[i]);

        for (int i = 0; i < 20; i++) {
            t.set(i, parsed.metaData.disengagePredictions.t[i]);
            brakeDisengageProbs.set(i, parsed.metaData.disengagePredictions.brakeDisengageProbs[i]);
            gasDesengageProbs.set(i, parsed.metaData.disengagePredictions.gasDesengageProbs[i]);
            steerOverrideProbs.set(i, parsed.metaData.disengagePredictions.steerOverrideProbs[i]);
            brake3MetersPerSecondSquaredProbs.set(i, parsed.metaData.disengagePredictions.brake3MetersPerSecondSquaredProbs[i]);
            brake4MetersPerSecondSquaredProbs.set(i, parsed.metaData.disengagePredictions.brake4MetersPerSecondSquaredProbs[i]);
            brake5MetersPerSecondSquaredProbs.set(i, parsed.metaData.disengagePredictions.brake5MetersPerSecondSquaredProbs[i]);
    }

        leads1.setProb(parsed.leads.get(0).prob);
        leads2.setProb(parsed.leads.get(1).prob);
        leads3.setProb(parsed.leads.get(2).prob);

        meta.setEngagedProb(parsed.metaData.engagedProb);
        meta.setHardBrakePredicted(parsed.metaData.hardBrakePredicted);
    }
}
