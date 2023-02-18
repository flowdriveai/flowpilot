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

import static ai.flow.modeld.CommonModel.MODEL_HEIGHT;
import static ai.flow.modeld.CommonModel.MODEL_WIDTH;

public class ImagePrepareCPU implements ImagePrepare{
    public int H;
    public int W;
    public Mat transformed, transformedYUV;
    public final INDArray transformedYUVNDArr = Nd4j.zeros(DataType.UINT8, MODEL_HEIGHT*3/2, MODEL_WIDTH, 1);
    public final INDArray netInputBuff = Nd4j.zeros(1, 12, MODEL_HEIGHT/2, MODEL_WIDTH/2);
    final Size outputSize = new Size(MODEL_WIDTH, MODEL_HEIGHT);
    public final INDArrayIndex[] imgTensor0Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(0,6),
            NDArrayIndex.all(), NDArrayIndex.all()};
    public final INDArrayIndex[] imgTensor1Slices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.interval(6,12),
            NDArrayIndex.all(), NDArrayIndex.all()};
    boolean rgb;

    public ImagePrepareCPU(int W, int H, boolean rgb){
        this.H = H;
        this.W = W;
        this.rgb = rgb;

        transformed = new Mat(MODEL_HEIGHT, MODEL_WIDTH, CvType.CV_8UC3);
        transformedYUV = new Mat(MODEL_HEIGHT*3/2, MODEL_WIDTH, CvType.CV_8UC1, transformedYUVNDArr.data().asNio());
    }

    public INDArray prepare(ByteBuffer imgBuffer, INDArray transform){
        Mat imageCurr = new Mat(H, W, CvType.CV_8UC3, imgBuffer);

        // shift current image to previous slot and process new image for current slot.
        netInputBuff.put(imgTensor0Slices, netInputBuff.get(imgTensor1Slices));

        Preprocess.TransformImg(imageCurr, transformed, transform, outputSize);
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
