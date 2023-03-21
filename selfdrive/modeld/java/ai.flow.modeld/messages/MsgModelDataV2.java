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
    public PrimitiveList.Float.Builder positionX;
    public PrimitiveList.Float.Builder positionY;
    public PrimitiveList.Float.Builder positionZ;
    public PrimitiveList.Float.Builder positionT;
    public PrimitiveList.Float.Builder positionXStd;
    public PrimitiveList.Float.Builder positionYStd;
    public PrimitiveList.Float.Builder positionZStd;

    public PrimitiveList.Float.Builder velocityX;
    public PrimitiveList.Float.Builder velocityY;
    public PrimitiveList.Float.Builder velocityZ;
    public PrimitiveList.Float.Builder velocityT;
    public PrimitiveList.Float.Builder velocityXStd;
    public PrimitiveList.Float.Builder velocityYStd;
    public PrimitiveList.Float.Builder velocityZStd;

    public PrimitiveList.Float.Builder orientationX;
    public PrimitiveList.Float.Builder orientationY;
    public PrimitiveList.Float.Builder orientationZ;
    public PrimitiveList.Float.Builder orientationT;
    public PrimitiveList.Float.Builder orientationXStd;
    public PrimitiveList.Float.Builder orientationYStd;
    public PrimitiveList.Float.Builder orientationZStd;

    public PrimitiveList.Float.Builder orientationRateX;
    public PrimitiveList.Float.Builder orientationRateY;
    public PrimitiveList.Float.Builder orientationRateZ;
    public PrimitiveList.Float.Builder orientationRateT;
    public PrimitiveList.Float.Builder orientationRateXStd;
    public PrimitiveList.Float.Builder orientationRateYStd;
    public PrimitiveList.Float.Builder orientationRateZStd;

    public StructList.Builder<Definitions.ModelDataV2.XYZTData.Builder> laneLines;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine1;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine2;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine3;
    public Definitions.ModelDataV2.XYZTData.Builder laneLine4;

    public PrimitiveList.Float.Builder laneLineX1;
    public PrimitiveList.Float.Builder laneLineY1;
    public PrimitiveList.Float.Builder laneLineZ1;
    public PrimitiveList.Float.Builder laneLineT1;
    public PrimitiveList.Float.Builder laneLineXStd1;
    public PrimitiveList.Float.Builder laneLineYStd1;
    public PrimitiveList.Float.Builder laneLineZStd1;
    public PrimitiveList.Float.Builder laneLine1Stds;

    public PrimitiveList.Float.Builder laneLineX2;
    public PrimitiveList.Float.Builder laneLineY2;
    public PrimitiveList.Float.Builder laneLineZ2;
    public PrimitiveList.Float.Builder laneLineT2;
    public PrimitiveList.Float.Builder laneLineXStd2;
    public PrimitiveList.Float.Builder laneLineYStd2;
    public PrimitiveList.Float.Builder laneLineZStd2;
    public PrimitiveList.Float.Builder laneLine2Stds;

    public PrimitiveList.Float.Builder laneLineX3;
    public PrimitiveList.Float.Builder laneLineY3;
    public PrimitiveList.Float.Builder laneLineZ3;
    public PrimitiveList.Float.Builder laneLineT3;
    public PrimitiveList.Float.Builder laneLineXStd3;
    public PrimitiveList.Float.Builder laneLineYStd3;
    public PrimitiveList.Float.Builder laneLineZStd3;
    public PrimitiveList.Float.Builder laneLine3Stds;

    public PrimitiveList.Float.Builder laneLineX4;
    public PrimitiveList.Float.Builder laneLineY4;
    public PrimitiveList.Float.Builder laneLineZ4;
    public PrimitiveList.Float.Builder laneLineT4;
    public PrimitiveList.Float.Builder laneLineXStd4;
    public PrimitiveList.Float.Builder laneLineYStd4;
    public PrimitiveList.Float.Builder laneLineZStd4;
    public PrimitiveList.Float.Builder laneLine4Stds;

    public PrimitiveList.Float.Builder laneLineProbs;
    public PrimitiveList.Float.Builder laneLineStds;

    public StructList.Builder<Definitions.ModelDataV2.XYZTData.Builder> roadEdges;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge1;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge2;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge3;
    public Definitions.ModelDataV2.XYZTData.Builder roadEdge4;

    public PrimitiveList.Float.Builder roadEdgeX1;
    public PrimitiveList.Float.Builder roadEdgeY1;
    public PrimitiveList.Float.Builder roadEdgeZ1;
    public PrimitiveList.Float.Builder roadEdgeT1;
    public PrimitiveList.Float.Builder roadEdgeXStd1;
    public PrimitiveList.Float.Builder roadEdgeYStd1;
    public PrimitiveList.Float.Builder roadEdgeZStd1;
    public PrimitiveList.Float.Builder roadEdge1Stds;

    public PrimitiveList.Float.Builder roadEdgeX2;
    public PrimitiveList.Float.Builder roadEdgeY2;
    public PrimitiveList.Float.Builder roadEdgeZ2;
    public PrimitiveList.Float.Builder roadEdgeT2;
    public PrimitiveList.Float.Builder roadEdgeXStd2;
    public PrimitiveList.Float.Builder roadEdgeYStd2;
    public PrimitiveList.Float.Builder roadEdgeZStd2;
    public PrimitiveList.Float.Builder roadEdge2Stds;

    public PrimitiveList.Float.Builder roadEdgeStds;

    public StructList.Builder<ai.flow.definitions.Definitions.ModelDataV2.LeadDataV3.Builder> leads;
    public Definitions.ModelDataV2.LeadDataV3.Builder leads1;
    public Definitions.ModelDataV2.LeadDataV3.Builder leads2;
    public Definitions.ModelDataV2.LeadDataV3.Builder leads3;

    public PrimitiveList.Float.Builder leadX1;
    public PrimitiveList.Float.Builder leadY1;
    public PrimitiveList.Float.Builder leadV1;
    public PrimitiveList.Float.Builder leadA1;
    public PrimitiveList.Float.Builder leadT1;
    public PrimitiveList.Float.Builder leadXStd1;
    public PrimitiveList.Float.Builder leadYStd1;
    public PrimitiveList.Float.Builder leadVStd1;
    public PrimitiveList.Float.Builder leadAStd1;

    public PrimitiveList.Float.Builder leadX2;
    public PrimitiveList.Float.Builder leadY2;
    public PrimitiveList.Float.Builder leadV2;
    public PrimitiveList.Float.Builder leadA2;
    public PrimitiveList.Float.Builder leadT2;
    public PrimitiveList.Float.Builder leadXStd2;
    public PrimitiveList.Float.Builder leadYStd2;
    public PrimitiveList.Float.Builder leadVStd2;
    public PrimitiveList.Float.Builder leadAStd2;

    public PrimitiveList.Float.Builder leadX3;
    public PrimitiveList.Float.Builder leadY3;
    public PrimitiveList.Float.Builder leadV3;
    public PrimitiveList.Float.Builder leadA3;
    public PrimitiveList.Float.Builder leadT3;
    public PrimitiveList.Float.Builder leadXStd3;
    public PrimitiveList.Float.Builder leadYStd3;
    public PrimitiveList.Float.Builder leadVStd3;
    public PrimitiveList.Float.Builder leadAStd3;

    public Definitions.ModelDataV2.MetaData.Builder meta;
    public Definitions.ModelDataV2.DisengagePredictions.Builder disengagePredictions;
    public PrimitiveList.Float.Builder desireState;
    public PrimitiveList.Float.Builder desirePredictions;
    public PrimitiveList.Float.Builder t;
    public PrimitiveList.Float.Builder brakeDisengageProbs;
    public PrimitiveList.Float.Builder gasDesengageProbs;
    public PrimitiveList.Float.Builder steerOverrideProbs;
    public PrimitiveList.Float.Builder brake3MetersPerSecondSquaredProbs;
    public PrimitiveList.Float.Builder brake4MetersPerSecondSquaredProbs;
    public PrimitiveList.Float.Builder brake5MetersPerSecondSquaredProbs;


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

        positionX = position.initX(CommonModel.TRAJECTORY_SIZE);
        positionY = position.initY(CommonModel.TRAJECTORY_SIZE);
        positionZ = position.initZ(CommonModel.TRAJECTORY_SIZE);
        positionT = position.initT(CommonModel.TRAJECTORY_SIZE);
        positionXStd = position.initXStd(CommonModel.TRAJECTORY_SIZE);
        positionYStd = position.initYStd(CommonModel.TRAJECTORY_SIZE);
        positionZStd = position.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        velocityX = velocity.initX(CommonModel.TRAJECTORY_SIZE);
        velocityY = velocity.initY(CommonModel.TRAJECTORY_SIZE);
        velocityZ = velocity.initZ(CommonModel.TRAJECTORY_SIZE);
        velocityT = velocity.initT(CommonModel.TRAJECTORY_SIZE);
        velocityXStd = velocity.initXStd(CommonModel.TRAJECTORY_SIZE);
        velocityYStd = velocity.initYStd(CommonModel.TRAJECTORY_SIZE);
        velocityZStd = velocity.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        orientationX = orientation.initX(CommonModel.TRAJECTORY_SIZE);
        orientationY = orientation.initY(CommonModel.TRAJECTORY_SIZE);
        orientationZ = orientation.initZ(CommonModel.TRAJECTORY_SIZE);
        orientationT = orientation.initT(CommonModel.TRAJECTORY_SIZE);
        orientationXStd = orientation.initXStd(CommonModel.TRAJECTORY_SIZE);
        orientationYStd = orientation.initYStd(CommonModel.TRAJECTORY_SIZE);
        orientationZStd = orientation.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        orientationRateX = orientationRate.initX(CommonModel.TRAJECTORY_SIZE);
        orientationRateY = orientationRate.initY(CommonModel.TRAJECTORY_SIZE);
        orientationRateZ = orientationRate.initZ(CommonModel.TRAJECTORY_SIZE);
        orientationRateT = orientationRate.initT(CommonModel.TRAJECTORY_SIZE);
        orientationRateXStd = orientationRate.initXStd(CommonModel.TRAJECTORY_SIZE);
        orientationRateYStd = orientationRate.initYStd(CommonModel.TRAJECTORY_SIZE);
        orientationRateZStd = orientationRate.initZStd(CommonModel.TRAJECTORY_SIZE);

        laneLines = modelDataV2.initLaneLines(4);
        laneLine1 = laneLines.get(0);
        laneLine2 = laneLines.get(1);
        laneLine3 = laneLines.get(2);
        laneLine4 = laneLines.get(3);

        laneLineX1 = laneLine1.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY1 = laneLine1.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ1 = laneLine1.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT1 = laneLine1.initT(CommonModel.TRAJECTORY_SIZE);
        laneLineXStd1 = laneLine1.initXStd(CommonModel.TRAJECTORY_SIZE);
        laneLineYStd1 = laneLine1.initYStd(CommonModel.TRAJECTORY_SIZE);
        laneLineZStd1 = laneLine1.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        laneLineX2 = laneLine2.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY2 = laneLine2.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ2 = laneLine2.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT2 = laneLine2.initT(CommonModel.TRAJECTORY_SIZE);
        laneLineXStd2 = laneLine2.initXStd(CommonModel.TRAJECTORY_SIZE);
        laneLineYStd2 = laneLine2.initYStd(CommonModel.TRAJECTORY_SIZE);
        laneLineZStd2 = laneLine2.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        laneLineX3 = laneLine3.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY3 = laneLine3.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ3 = laneLine3.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT3 = laneLine3.initT(CommonModel.TRAJECTORY_SIZE);
        laneLineXStd3 = laneLine3.initXStd(CommonModel.TRAJECTORY_SIZE);
        laneLineYStd3 = laneLine3.initYStd(CommonModel.TRAJECTORY_SIZE);
        laneLineZStd3 = laneLine3.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        laneLineX4 = laneLine4.initX(CommonModel.TRAJECTORY_SIZE);
        laneLineY4 = laneLine4.initY(CommonModel.TRAJECTORY_SIZE);
        laneLineZ4 = laneLine4.initZ(CommonModel.TRAJECTORY_SIZE);
        laneLineT4 = laneLine4.initT(CommonModel.TRAJECTORY_SIZE);
        laneLineXStd4 = laneLine4.initXStd(CommonModel.TRAJECTORY_SIZE);
        laneLineYStd4 = laneLine4.initYStd(CommonModel.TRAJECTORY_SIZE);
        laneLineZStd4 = laneLine4.initZStd(CommonModel.TRAJECTORY_SIZE);

        laneLineProbs = modelDataV2.initLaneLineProbs(4);
        laneLineStds = modelDataV2.initLaneLineStds(4);
        
        roadEdges = modelDataV2.initRoadEdges(2);
        roadEdge1 = roadEdges.get(0);
        roadEdge2 = roadEdges.get(1);

        roadEdgeX1 = roadEdge1.initX(CommonModel.TRAJECTORY_SIZE);
        roadEdgeY1 = roadEdge1.initY(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZ1 = roadEdge1.initZ(CommonModel.TRAJECTORY_SIZE);
        roadEdgeT1 = roadEdge1.initT(CommonModel.TRAJECTORY_SIZE);
        roadEdgeXStd1 = roadEdge1.initXStd(CommonModel.TRAJECTORY_SIZE);
        roadEdgeYStd1 = roadEdge1.initYStd(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZStd1 = roadEdge1.initZStd(CommonModel.TRAJECTORY_SIZE);
        
        roadEdgeX2 = roadEdge2.initX(CommonModel.TRAJECTORY_SIZE);
        roadEdgeY2 = roadEdge2.initY(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZ2 = roadEdge2.initZ(CommonModel.TRAJECTORY_SIZE);
        roadEdgeT2 = roadEdge2.initT(CommonModel.TRAJECTORY_SIZE);
        roadEdgeXStd2 = roadEdge2.initXStd(CommonModel.TRAJECTORY_SIZE);
        roadEdgeYStd2 = roadEdge2.initYStd(CommonModel.TRAJECTORY_SIZE);
        roadEdgeZStd2 = roadEdge2.initZStd(CommonModel.TRAJECTORY_SIZE);

        roadEdgeStds = modelDataV2.initRoadEdgeStds(2);
        
        leads = modelDataV2.initLeadsV3(CommonModel.LEAD_MHP_SELECTION);
        leads1 = leads.get(0);
        leads2 = leads.get(1);
        leads3 = leads.get(2);

        leadX1 = leads1.initX(CommonModel.LEAD_TRAJ_LEN);
        leadY1 = leads1.initY(CommonModel.LEAD_TRAJ_LEN);
        leadV1 = leads1.initV(CommonModel.LEAD_TRAJ_LEN);
        leadA1 = leads1.initA(CommonModel.LEAD_TRAJ_LEN);
        leadT1 = leads1.initT(CommonModel.LEAD_TRAJ_LEN);
        leadXStd1 = leads1.initXStd(CommonModel.LEAD_TRAJ_LEN);
        leadYStd1 = leads1.initYStd(CommonModel.LEAD_TRAJ_LEN);
        leadVStd1 = leads1.initVStd(CommonModel.LEAD_TRAJ_LEN);
        leadAStd1 = leads1.initAStd(CommonModel.LEAD_TRAJ_LEN);

        leadX2 = leads2.initX(CommonModel.LEAD_TRAJ_LEN);
        leadY2 = leads2.initY(CommonModel.LEAD_TRAJ_LEN);
        leadV2 = leads2.initV(CommonModel.LEAD_TRAJ_LEN);
        leadA2 = leads2.initA(CommonModel.LEAD_TRAJ_LEN);
        leadT2 = leads2.initT(CommonModel.LEAD_TRAJ_LEN);
        leadXStd2 = leads2.initXStd(CommonModel.LEAD_TRAJ_LEN);
        leadYStd2 = leads2.initYStd(CommonModel.LEAD_TRAJ_LEN);
        leadVStd2 = leads2.initVStd(CommonModel.LEAD_TRAJ_LEN);
        leadAStd2 = leads2.initAStd(CommonModel.LEAD_TRAJ_LEN);

        leadX3 = leads3.initX(CommonModel.LEAD_TRAJ_LEN);
        leadY3 = leads3.initY(CommonModel.LEAD_TRAJ_LEN);
        leadV3 = leads3.initV(CommonModel.LEAD_TRAJ_LEN);
        leadA3 = leads3.initA(CommonModel.LEAD_TRAJ_LEN);
        leadT3 = leads3.initT(CommonModel.LEAD_TRAJ_LEN);
        leadXStd3 = leads3.initXStd(CommonModel.LEAD_TRAJ_LEN);
        leadYStd3 = leads3.initYStd(CommonModel.LEAD_TRAJ_LEN);
        leadVStd3 = leads3.initVStd(CommonModel.LEAD_TRAJ_LEN);
        leadAStd3 = leads3.initAStd(CommonModel.LEAD_TRAJ_LEN);

        meta = modelDataV2.initMeta();
        disengagePredictions = meta.initDisengagePredictions();

        desireState = meta.initDesireState(CommonModel.DESIRE_LEN);
        desirePredictions = meta.initDesirePrediction(4*CommonModel.DESIRE_LEN);
        t = disengagePredictions.initT(5);
        gasDesengageProbs = disengagePredictions.initGasDisengageProbs(CommonModel.NUM_META_INTERVALS);
        brakeDisengageProbs = disengagePredictions.initBrakeDisengageProbs(CommonModel.NUM_META_INTERVALS);
        steerOverrideProbs = disengagePredictions.initSteerOverrideProbs(CommonModel.NUM_META_INTERVALS);
        brake3MetersPerSecondSquaredProbs = disengagePredictions.initBrake3MetersPerSecondSquaredProbs(CommonModel.NUM_META_INTERVALS);
        brake4MetersPerSecondSquaredProbs = disengagePredictions.initBrake4MetersPerSecondSquaredProbs(CommonModel.NUM_META_INTERVALS);
        brake5MetersPerSecondSquaredProbs = disengagePredictions.initBrake5MetersPerSecondSquaredProbs(CommonModel.NUM_META_INTERVALS);
    }

    public static void fillParsed(ParsedOutputs parsed, Definitions.ModelDataV2.Reader msg, boolean full) { // TODO Avoid this
        for (int i = 0; i < CommonModel.TRAJECTORY_SIZE; i++) {
            parsed.position.get(0)[i] = msg.getPosition().getX().get(i);
            parsed.position.get(1)[i] = msg.getPosition().getY().get(i);
            parsed.position.get(2)[i] = msg.getPosition().getZ().get(i);
            parsed.position.get(3)[i] = msg.getPosition().getT().get(i);
            parsed.position.get(4)[i] = msg.getPosition().getXStd().get(i);
            parsed.position.get(5)[i] = msg.getPosition().getYStd().get(i);
            parsed.position.get(6)[i] = msg.getPosition().getZStd().get(i);

            parsed.velocity.get(0)[i] = msg.getVelocity().getX().get(i);
            parsed.velocity.get(1)[i] = msg.getVelocity().getY().get(i);
            parsed.velocity.get(2)[i] = msg.getVelocity().getZ().get(i);
            parsed.velocity.get(3)[i] = msg.getVelocity().getT().get(i);
            parsed.velocity.get(4)[i] = msg.getVelocity().getXStd().get(i);
            parsed.velocity.get(5)[i] = msg.getVelocity().getYStd().get(i);
            parsed.velocity.get(6)[i] = msg.getVelocity().getZStd().get(i);

            parsed.orientation.get(0)[i] = msg.getOrientation().getX().get(i);
            parsed.orientation.get(1)[i] = msg.getOrientation().getY().get(i);
            parsed.orientation.get(2)[i] = msg.getOrientation().getZ().get(i);
            parsed.orientation.get(3)[i] = msg.getOrientation().getT().get(i);
            parsed.orientation.get(4)[i] = msg.getOrientation().getXStd().get(i);
            parsed.orientation.get(5)[i] = msg.getOrientation().getYStd().get(i);
            parsed.orientation.get(6)[i] = msg.getOrientation().getZStd().get(i);

            parsed.orientationRate.get(0)[i] = msg.getOrientationRate().getX().get(i);
            parsed.orientationRate.get(1)[i] = msg.getOrientationRate().getY().get(i);
            parsed.orientationRate.get(2)[i] = msg.getOrientationRate().getZ().get(i);
            parsed.orientationRate.get(3)[i] = msg.getOrientationRate().getT().get(i);
            parsed.orientationRate.get(4)[i] = msg.getOrientationRate().getXStd().get(i);
            parsed.orientationRate.get(5)[i] = msg.getOrientationRate().getYStd().get(i);
            parsed.orientationRate.get(6)[i] = msg.getOrientationRate().getZStd().get(i);

            if (full) {
                parsed.laneLines.get(0).get(0)[i] = msg.getLaneLines().get(0).getX().get(i);
                parsed.laneLines.get(0).get(1)[i] = msg.getLaneLines().get(0).getY().get(i);
                parsed.laneLines.get(0).get(2)[i] = msg.getLaneLines().get(0).getZ().get(i);
                parsed.laneLines.get(0).get(3)[i] = msg.getLaneLines().get(0).getT().get(i);
                parsed.laneLines.get(0).get(4)[i] = msg.getLaneLines().get(0).getXStd().get(i);
                parsed.laneLines.get(0).get(5)[i] = msg.getLaneLines().get(0).getYStd().get(i);
                parsed.laneLines.get(0).get(6)[i] = msg.getLaneLines().get(0).getZStd().get(i);

                parsed.laneLines.get(1).get(0)[i] = msg.getLaneLines().get(1).getX().get(i);
                parsed.laneLines.get(1).get(1)[i] = msg.getLaneLines().get(1).getY().get(i);
                parsed.laneLines.get(1).get(2)[i] = msg.getLaneLines().get(1).getZ().get(i);
                parsed.laneLines.get(1).get(3)[i] = msg.getLaneLines().get(1).getT().get(i);
                parsed.laneLines.get(1).get(4)[i] = msg.getLaneLines().get(1).getXStd().get(i);
                parsed.laneLines.get(1).get(5)[i] = msg.getLaneLines().get(1).getYStd().get(i);
                parsed.laneLines.get(1).get(6)[i] = msg.getLaneLines().get(1).getZStd().get(i);

                parsed.laneLines.get(2).get(0)[i] = msg.getLaneLines().get(2).getX().get(i);
                parsed.laneLines.get(2).get(1)[i] = msg.getLaneLines().get(2).getY().get(i);
                parsed.laneLines.get(2).get(2)[i] = msg.getLaneLines().get(2).getZ().get(i);
                parsed.laneLines.get(2).get(3)[i] = msg.getLaneLines().get(2).getT().get(i);
                parsed.laneLines.get(2).get(4)[i] = msg.getLaneLines().get(2).getXStd().get(i);
                parsed.laneLines.get(2).get(5)[i] = msg.getLaneLines().get(2).getYStd().get(i);
                parsed.laneLines.get(2).get(6)[i] = msg.getLaneLines().get(2).getZStd().get(i);

                parsed.laneLines.get(3).get(0)[i] = msg.getLaneLines().get(3).getX().get(i);
                parsed.laneLines.get(3).get(1)[i] = msg.getLaneLines().get(3).getY().get(i);
                parsed.laneLines.get(3).get(2)[i] = msg.getLaneLines().get(3).getZ().get(i);
                parsed.laneLines.get(3).get(3)[i] = msg.getLaneLines().get(3).getT().get(i);
                parsed.laneLines.get(3).get(4)[i] = msg.getLaneLines().get(3).getXStd().get(i);
                parsed.laneLines.get(3).get(5)[i] = msg.getLaneLines().get(3).getYStd().get(i);
                parsed.laneLines.get(3).get(6)[i] = msg.getLaneLines().get(3).getZStd().get(i);

                parsed.roadEdges.get(0).get(0)[i] = msg.getRoadEdges().get(0).getX().get(i);
                parsed.roadEdges.get(0).get(1)[i] = msg.getRoadEdges().get(0).getY().get(i);
                parsed.roadEdges.get(0).get(2)[i] = msg.getRoadEdges().get(0).getZ().get(i);
                parsed.roadEdges.get(0).get(3)[i] = msg.getRoadEdges().get(0).getT().get(i);
                parsed.roadEdges.get(0).get(4)[i] = msg.getRoadEdges().get(0).getXStd().get(i);
                parsed.roadEdges.get(0).get(5)[i] = msg.getRoadEdges().get(0).getYStd().get(i);
                parsed.roadEdges.get(0).get(6)[i] = msg.getRoadEdges().get(0).getZStd().get(i);

                parsed.roadEdges.get(1).get(0)[i] = msg.getRoadEdges().get(1).getX().get(i);
                parsed.roadEdges.get(1).get(1)[i] = msg.getRoadEdges().get(1).getY().get(i);
                parsed.roadEdges.get(1).get(2)[i] = msg.getRoadEdges().get(1).getZ().get(i);
                parsed.roadEdges.get(1).get(3)[i] = msg.getRoadEdges().get(1).getT().get(i);
                parsed.roadEdges.get(1).get(4)[i] = msg.getRoadEdges().get(1).getXStd().get(i);
                parsed.roadEdges.get(1).get(5)[i] = msg.getRoadEdges().get(1).getYStd().get(i);
                parsed.roadEdges.get(1).get(6)[i] = msg.getRoadEdges().get(1).getZStd().get(i);
            }
        }

        if (full) {
            for (int i = 0; i < 4; i++) {
                parsed.laneLineProbs[i] = msg.getLaneLineProbs().get(i);
                parsed.laneLineStds[i] = msg.getLaneLineStds().get(i);
            }
    
            for (int i = 0; i < 2; i++)
                parsed.roadEdgeStds[i] = msg.getRoadEdgeStds().get(i);
        }

        for (int i = 0; i < CommonModel.DESIRE_LEN; i++)
            parsed.metaData.desireState[i] = msg.getMeta().getDesireState().get(i);

        for (int i = 0; i < 4 * CommonModel.DESIRE_LEN; i++)
            parsed.metaData.desirePrediction[i] = msg.getMeta().getDesirePrediction().get(i);
        
        for (int i = 0; i < 5; i++)
            parsed.metaData.disengagePredictions.t[i] = msg.getMeta().getDisengagePredictions().getT().get(i);

        for (int i = 0; i < CommonModel.NUM_META_INTERVALS; i++) {
            parsed.metaData.disengagePredictions.brakeDisengageProbs[i] = msg.getMeta().getDisengagePredictions().getBrakeDisengageProbs().get(i);
            parsed.metaData.disengagePredictions.gasDesengageProbs[i] = msg.getMeta().getDisengagePredictions().getGasDisengageProbs().get(i);
            parsed.metaData.disengagePredictions.steerOverrideProbs[i] = msg.getMeta().getDisengagePredictions().getSteerOverrideProbs().get(i);
            parsed.metaData.disengagePredictions.brake3MetersPerSecondSquaredProbs[i] = msg.getMeta().getDisengagePredictions().getBrake3MetersPerSecondSquaredProbs().get(i);
            parsed.metaData.disengagePredictions.brake4MetersPerSecondSquaredProbs[i] = msg.getMeta().getDisengagePredictions().getBrake4MetersPerSecondSquaredProbs().get(i);
            parsed.metaData.disengagePredictions.brake5MetersPerSecondSquaredProbs[i] = msg.getMeta().getDisengagePredictions().getBrake5MetersPerSecondSquaredProbs().get(i);
        }

        parsed.metaData.engagedProb = msg.getMeta().getEngagedProb();
        parsed.metaData.hardBrakePredicted = msg.getMeta().getHardBrakePredicted();

        for (int i = 0; i < CommonModel.LEAD_TRAJ_LEN; i++) {

            parsed.leads.get(0).x[i] = msg.getLeadsV3().get(0).getX().get(i);
            parsed.leads.get(0).y[i] = msg.getLeadsV3().get(0).getY().get(i);
            parsed.leads.get(0).v[i] = msg.getLeadsV3().get(0).getV().get(i);
            parsed.leads.get(0).a[i] = msg.getLeadsV3().get(0).getA().get(i);
            parsed.leads.get(0).XStd[i] = msg.getLeadsV3().get(0).getXStd().get(i);
            parsed.leads.get(0).YStd[i] = msg.getLeadsV3().get(0).getYStd().get(i);
            parsed.leads.get(0).VStd[i] = msg.getLeadsV3().get(0).getVStd().get(i);
            parsed.leads.get(0).AStd[i] = msg.getLeadsV3().get(0).getAStd().get(i);

            parsed.leads.get(1).x[i] = msg.getLeadsV3().get(1).getX().get(i);
            parsed.leads.get(1).y[i] = msg.getLeadsV3().get(1).getY().get(i);
            parsed.leads.get(1).v[i] = msg.getLeadsV3().get(1).getV().get(i);
            parsed.leads.get(1).a[i] = msg.getLeadsV3().get(1).getA().get(i);
            parsed.leads.get(1).XStd[i] = msg.getLeadsV3().get(1).getXStd().get(i);
            parsed.leads.get(1).YStd[i] = msg.getLeadsV3().get(1).getYStd().get(i);
            parsed.leads.get(1).VStd[i] = msg.getLeadsV3().get(1).getVStd().get(i);
            parsed.leads.get(1).AStd[i] = msg.getLeadsV3().get(1).getAStd().get(i);

            parsed.leads.get(2).x[i] = msg.getLeadsV3().get(2).getX().get(i);
            parsed.leads.get(2).y[i] = msg.getLeadsV3().get(2).getY().get(i);
            parsed.leads.get(2).v[i] = msg.getLeadsV3().get(2).getV().get(i);
            parsed.leads.get(2).a[i] = msg.getLeadsV3().get(2).getA().get(i);
            parsed.leads.get(2).XStd[i] = msg.getLeadsV3().get(2).getXStd().get(i);
            parsed.leads.get(2).YStd[i] = msg.getLeadsV3().get(2).getYStd().get(i);
            parsed.leads.get(2).VStd[i] = msg.getLeadsV3().get(2).getVStd().get(i);
            parsed.leads.get(2).AStd[i] = msg.getLeadsV3().get(2).getAStd().get(i);
        }

        for (int i = 0; i < 5; i++) {
            parsed.leads.get(0).t[i] = msg.getLeadsV3().get(0).getT().get(i);
            parsed.leads.get(1).t[i] = msg.getLeadsV3().get(0).getT().get(i);
            parsed.leads.get(2).t[i] = msg.getLeadsV3().get(0).getT().get(i);
        }

        parsed.leads.get(0).prob = msg.getLeadsV3().get(0).getProb();
        parsed.leads.get(1).prob = msg.getLeadsV3().get(1).getProb();
        parsed.leads.get(2).prob = msg.getLeadsV3().get(2).getProb();

        parsed.leads.get(0).probTime = msg.getLeadsV3().get(0).getProbTime();
        parsed.leads.get(1).probTime = msg.getLeadsV3().get(1).getProbTime();
        parsed.leads.get(2).probTime = msg.getLeadsV3().get(2).getProbTime();
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
            positionXStd.set(i, parsed.position.get(4)[i]);
            positionYStd.set(i, parsed.position.get(5)[i]);
            positionZStd.set(i, parsed.position.get(6)[i]);

            velocityX.set(i, parsed.velocity.get(0)[i]);
            velocityY.set(i, parsed.velocity.get(1)[i]);
            velocityZ.set(i, parsed.velocity.get(2)[i]);
            velocityT.set(i, parsed.velocity.get(3)[i]);
            velocityXStd.set(i, parsed.velocity.get(4)[i]);
            velocityYStd.set(i, parsed.velocity.get(5)[i]);
            velocityZStd.set(i, parsed.velocity.get(6)[i]);

            orientationX.set(i, parsed.orientation.get(0)[i]);
            orientationY.set(i, parsed.orientation.get(1)[i]);
            orientationZ.set(i, parsed.orientation.get(2)[i]);
            orientationT.set(i, parsed.orientation.get(3)[i]);
            orientationXStd.set(i, parsed.orientation.get(4)[i]);
            orientationYStd.set(i, parsed.orientation.get(5)[i]);
            orientationZStd.set(i, parsed.orientation.get(6)[i]);

            orientationRateX.set(i, parsed.orientationRate.get(0)[i]);
            orientationRateY.set(i, parsed.orientationRate.get(1)[i]);
            orientationRateZ.set(i, parsed.orientationRate.get(2)[i]);
            orientationRateT.set(i, parsed.orientationRate.get(3)[i]);
            orientationRateXStd.set(i, parsed.orientationRate.get(4)[i]);
            orientationRateYStd.set(i, parsed.orientationRate.get(5)[i]);
            orientationRateZStd.set(i, parsed.orientationRate.get(6)[i]);

            laneLineX1.set(i, parsed.laneLines.get(0).get(0)[i]);
            laneLineY1.set(i, parsed.laneLines.get(0).get(1)[i]);
            laneLineZ1.set(i, parsed.laneLines.get(0).get(2)[i]);
            laneLineT1.set(i, parsed.laneLines.get(0).get(3)[i]);
            laneLineXStd1.set(i, parsed.laneLines.get(0).get(4)[i]);
            laneLineYStd1.set(i, parsed.laneLines.get(0).get(5)[i]);
            laneLineZStd1.set(i, parsed.laneLines.get(0).get(6)[i]);

            laneLineX2.set(i, parsed.laneLines.get(1).get(0)[i]);
            laneLineY2.set(i, parsed.laneLines.get(1).get(1)[i]);
            laneLineZ2.set(i, parsed.laneLines.get(1).get(2)[i]);
            laneLineT2.set(i, parsed.laneLines.get(1).get(3)[i]);
            laneLineXStd2.set(i, parsed.laneLines.get(1).get(4)[i]);
            laneLineYStd2.set(i, parsed.laneLines.get(1).get(5)[i]);
            laneLineZStd2.set(i, parsed.laneLines.get(1).get(6)[i]);

            laneLineX3.set(i, parsed.laneLines.get(2).get(0)[i]);
            laneLineY3.set(i, parsed.laneLines.get(2).get(1)[i]);
            laneLineZ3.set(i, parsed.laneLines.get(2).get(2)[i]);
            laneLineT3.set(i, parsed.laneLines.get(2).get(3)[i]);
            laneLineXStd3.set(i, parsed.laneLines.get(2).get(4)[i]);
            laneLineYStd3.set(i, parsed.laneLines.get(2).get(5)[i]);
            laneLineZStd3.set(i, parsed.laneLines.get(2).get(6)[i]);

            laneLineX4.set(i, parsed.laneLines.get(3).get(0)[i]);
            laneLineY4.set(i, parsed.laneLines.get(3).get(1)[i]);
            laneLineZ4.set(i, parsed.laneLines.get(3).get(2)[i]);
            laneLineT4.set(i, parsed.laneLines.get(3).get(3)[i]);
            laneLineXStd4.set(i, parsed.laneLines.get(3).get(4)[i]);
            laneLineYStd4.set(i, parsed.laneLines.get(3).get(5)[i]);
            laneLineZStd4.set(i, parsed.laneLines.get(3).get(6)[i]);

            roadEdgeX1.set(i, parsed.roadEdges.get(0).get(0)[i]);
            roadEdgeY1.set(i, parsed.roadEdges.get(0).get(1)[i]);
            roadEdgeZ1.set(i, parsed.roadEdges.get(0).get(2)[i]);
            roadEdgeT1.set(i, parsed.roadEdges.get(0).get(3)[i]);
            roadEdgeXStd1.set(i, parsed.roadEdges.get(0).get(4)[i]);
            roadEdgeYStd1.set(i, parsed.roadEdges.get(0).get(5)[i]);
            roadEdgeZStd1.set(i, parsed.roadEdges.get(0).get(6)[i]);

            roadEdgeX2.set(i, parsed.roadEdges.get(1).get(0)[i]);
            roadEdgeY2.set(i, parsed.roadEdges.get(1).get(1)[i]);
            roadEdgeZ2.set(i, parsed.roadEdges.get(1).get(2)[i]);
            roadEdgeT2.set(i, parsed.roadEdges.get(1).get(3)[i]);
            roadEdgeXStd2.set(i, parsed.roadEdges.get(1).get(4)[i]);
            roadEdgeYStd2.set(i, parsed.roadEdges.get(1).get(5)[i]);
            roadEdgeZStd2.set(i, parsed.roadEdges.get(1).get(6)[i]);
        }

        for (int i = 0; i < 4; i++) {
            laneLineProbs.set(i, parsed.laneLineProbs[i]);
            laneLineStds.set(i, parsed.laneLineStds[i]);
        }

        for (int i = 0; i < 2; i++)
            roadEdgeStds.set(i, parsed.roadEdgeStds[i]);
        
        for (int i = 0; i < Parser.DESIRE_LEN; i++)
            desireState.set(i, parsed.metaData.desireState[i]);

        for (int i = 0; i < 4 * Parser.DESIRE_LEN; i++)
            desirePredictions.set(i, parsed.metaData.desirePrediction[i]);

        for (int i = 0; i < Parser.NUM_META_INTERVALS; i++)  {
            t.set(i, parsed.metaData.disengagePredictions.t[i]);
            brakeDisengageProbs.set(i, parsed.metaData.disengagePredictions.brakeDisengageProbs[i]);
            gasDesengageProbs.set(i, parsed.metaData.disengagePredictions.gasDesengageProbs[i]);
            steerOverrideProbs.set(i, parsed.metaData.disengagePredictions.steerOverrideProbs[i]);
            brake3MetersPerSecondSquaredProbs.set(i, parsed.metaData.disengagePredictions.brake3MetersPerSecondSquaredProbs[i]);
            brake4MetersPerSecondSquaredProbs.set(i, parsed.metaData.disengagePredictions.brake4MetersPerSecondSquaredProbs[i]);
            brake5MetersPerSecondSquaredProbs.set(i, parsed.metaData.disengagePredictions.brake5MetersPerSecondSquaredProbs[i]);
        }

        meta.setEngagedProb(parsed.metaData.engagedProb);
        meta.setHardBrakePredicted(parsed.metaData.hardBrakePredicted);

        for (int i = 0; i < Parser.LEAD_TRAJ_LEN; i++) {
            leadX1.set(i, parsed.leads.get(0).x[i]);
            leadY1.set(i, parsed.leads.get(0).y[i]);
            leadV1.set(i, parsed.leads.get(0).v[i]);
            leadA1.set(i, parsed.leads.get(0).a[i]);
            leadXStd1.set(i, parsed.leads.get(0).XStd[i]);
            leadYStd1.set(i, parsed.leads.get(0).YStd[i]);
            leadVStd1.set(i, parsed.leads.get(0).VStd[i]);
            leadAStd1.set(i, parsed.leads.get(0).AStd[i]);

            leadX2.set(i, parsed.leads.get(1).x[i]);
            leadY2.set(i, parsed.leads.get(1).y[i]);
            leadV2.set(i, parsed.leads.get(1).v[i]);
            leadA2.set(i, parsed.leads.get(1).a[i]);
            leadXStd2.set(i, parsed.leads.get(1).XStd[i]);
            leadYStd2.set(i, parsed.leads.get(1).YStd[i]);
            leadVStd2.set(i, parsed.leads.get(1).VStd[i]);
            leadAStd2.set(i, parsed.leads.get(1).AStd[i]);

            leadX3.set(i, parsed.leads.get(2).x[i]);
            leadY3.set(i, parsed.leads.get(2).y[i]);
            leadV3.set(i, parsed.leads.get(2).v[i]);
            leadA3.set(i, parsed.leads.get(2).a[i]);
            leadXStd3.set(i, parsed.leads.get(2).XStd[i]);
            leadYStd3.set(i, parsed.leads.get(2).YStd[i]);
            leadVStd3.set(i, parsed.leads.get(2).VStd[i]);
            leadAStd3.set(i, parsed.leads.get(2).AStd[i]);
        }

        for (int i = 0; i < 5; i++) {
            leadT1.set(i, parsed.leads.get(0).t[i]);
            leadT2.set(i, parsed.leads.get(1).t[i]);
            leadT3.set(i, parsed.leads.get(2).t[i]);
        }

        leads1.setProb(parsed.leads.get(0).prob);
        leads2.setProb(parsed.leads.get(1).prob);
        leads3.setProb(parsed.leads.get(2).prob);

        leads1.setProbTime(parsed.leads.get(0).probTime);
        leads2.setProbTime(parsed.leads.get(1).probTime);
        leads3.setProbTime(parsed.leads.get(2).probTime);
    }
}
