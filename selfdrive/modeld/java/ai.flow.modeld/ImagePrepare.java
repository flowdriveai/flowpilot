package ai.flow.modeld;

import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.nio.ByteBuffer;

public class ImagePrepare {
    public int H;
    public int W;
    public int MODEL_WIDTH = 512;
    public int MODEL_HEIGHT = 256;
    public int MODEL_FRAME_SIZE = MODEL_WIDTH * MODEL_HEIGHT * 3 / 2;
    public int BUF_SIZE = MODEL_FRAME_SIZE * 2;
    public Mat transformed, transformedYUV;
    public final INDArray transformedYUVNDArr = Nd4j.zeros(DataType.UINT8, MODEL_HEIGHT*3/2, MODEL_WIDTH, 1);
    public final INDArray netInputBuff = Nd4j.zeros(1, 12, MODEL_HEIGHT/2, MODEL_WIDTH/2);
    final Size outputSize = new Size(MODEL_WIDTH, MODEL_HEIGHT);
    public final INDArrayIndex[] imgTensor0Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0,6),
            NDArrayIndex.all(), NDArrayIndex.all()};
    public final INDArrayIndex[] imgTensor1Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(6,12),
            NDArrayIndex.all(), NDArrayIndex.all()};

    public ImagePrepare(int W, int H){
        this.H = H;
        this.W = W;

        transformed = new Mat(MODEL_HEIGHT, MODEL_WIDTH, CvType.CV_8UC3);
        transformedYUV = new Mat(MODEL_HEIGHT*3/2, MODEL_WIDTH, CvType.CV_8UC1, transformedYUVNDArr.data().asNio());
    }

    public INDArray prepare(ByteBuffer imgBuffer, INDArray deviceToCalibTransform, boolean rgb){
        Mat imageCurr = new Mat(H, W, CvType.CV_8UC3, imgBuffer);

        // shift current image to previous slot and process new image for current slot.
        netInputBuff.put(imgTensor0Slices, netInputBuff.get(imgTensor1Slices));

        Preprocess.TransformImg(imageCurr, transformed, deviceToCalibTransform, outputSize);
        Preprocess.RGB888toYUV420(transformed, transformedYUV);
        Preprocess.YUV420toTensor(transformedYUVNDArr, netInputBuff, 1);

        imageCurr.release();
        return netInputBuff;
    }

    public void dispose(){
        transformedYUVNDArr.close();
        netInputBuff.close();
        transformed.release();
        transformedYUV.release();
    }
}
