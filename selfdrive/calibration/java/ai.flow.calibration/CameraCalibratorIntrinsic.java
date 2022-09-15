package ai.flow.calibration;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ai.flow.common.ParamsInterface;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

public class CameraCalibratorIntrinsic implements Runnable {
    public static int STATUS_UNCALIBRATED = 0;
    public static int STATUS_SUCCESS = 1;
    public static int STATUS_ERROR = 2;
    public List<Mat> objectPoints;
    public Size imageSize;
    public Mat cameraMatrix = new Mat(), distortionCoefficients = new Mat();
    public List<Mat> rotationMatrices = new ArrayList<>(), translationVectors = new ArrayList<>();
    public List<Mat> imagePoints = new ArrayList<>();
    public Size patternSize;
    public float patternScale = 20;
    public Thread thread;
    public String threadName = "calibrationIntrinsic";
    public int status = STATUS_UNCALIBRATED;
    public float THRESHOLD_ERROR = 50f;
    Mat cachedImagePoints = new Mat();
    ParamsInterface params;
    Size winSize = new Size(7, 7);
    Size zeroZone = new Size(-1, -1);
    TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 40, 0.001);

    public CameraCalibratorIntrinsic(int patternColumns, int patternRows, ParamsInterface params) {
        patternSize = new Size(patternColumns, patternRows);
        this.params = params;
    }

    public boolean isValidRMSE(Mat currentImagePoints) {
        for (Mat prevImagePoints : imagePoints){
            assert prevImagePoints.rows() == currentImagePoints.rows() : "Chessboard size not consistent";
            float rmse = 0;
            for (int i = 0; i < currentImagePoints.rows(); i++)
                rmse += Math.pow(currentImagePoints.get(i, 0)[0] - prevImagePoints.get(i, 0)[0], 2) + Math.pow(currentImagePoints.get(i, 0)[1] - prevImagePoints.get(i, 0)[1], 2);
            rmse = (float) Math.sqrt(rmse / currentImagePoints.rows());
            if (rmse < THRESHOLD_ERROR)
                return false;
        }
        return true;
    }

    public static List<Mat> getObjectPoints(int size, Size patternSize, float scale) {
        List<Mat> objectPoints = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            objectPoints.add(worldCoordinatesChess(patternSize, scale));
        }
        return objectPoints;
    }

    public static MatOfPoint3f worldCoordinatesChess(Size patternSize, float scale) {
        MatOfPoint3f objectPoint = new MatOfPoint3f();
        List<Point3> objectPoints = new ArrayList<>();
        for (int row = 0; row < patternSize.height; row++) {
            for (int col = 0; col < patternSize.width; col++) {
                objectPoints.add(scaledWorldPoint(row, col, scale));
            }
        }
        objectPoint.fromList(objectPoints);
        return objectPoint;
    }

    public static Point3 scaledWorldPoint(int row, int col, float scale) {
        return new Point3(col * scale, row * scale, 0.0);
    }

    public boolean addImage(Mat image) {
        if (imageSize == null)
            imageSize = image.size();
        else if (!imageSize.equals(image.size())) {
            System.err.println("add images of same size: " + imageSize.height + "x" + imageSize.width);
            return false;
        }
        return findChessboardCorners(image);
    }

    public void removeLast() {
        if (imagePoints.size() > 0)
            imagePoints.remove(imagePoints.size() - 1);
    }

    public int currentImagePoints() {
        return imagePoints.size();
    }

    public boolean findChessboardCorners(Mat inputMat) {
        MatOfPoint2f chessCornersImage = new MatOfPoint2f();

        Mat gray = new Mat();
        Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY);
        boolean canFindChessboard = Calib3d.findChessboardCorners(inputMat, patternSize, chessCornersImage);
        if (!canFindChessboard) {
            gray.release();
            return false;
        }
        Imgproc.cornerSubPix(gray, chessCornersImage, winSize, zeroZone, criteria);
        gray.release();
        if (!isValidRMSE(chessCornersImage)) // compare if orientations are not too similar to previous ones.
            return false;
        imagePoints.add(chessCornersImage);
        return true;
    }

    public void calibrate() {
        run();
    }

    public void calibrateBackground() {
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.setDaemon(false);
            thread.start();
        }
    }

    public static byte[] floatToByte(float[] input) {
        byte[] ret = new byte[input.length*4];
        for (int x = 0; x < input.length; x++) {
            ByteBuffer.wrap(ret, x*4, 4).putFloat(input[x]);
        }
        return ret;
    }

    public static float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length/4];
        for (int x = 0; x < input.length; x+=4) {
            ret[x/4] = ByteBuffer.wrap(input, x, 4).getFloat();
        }
        return ret;
    }

    public void run() {
        if (imagePoints.size() == 0) {
            System.err.println("Add some images before calibrating.");
            return;
        }
        try {
            objectPoints = getObjectPoints(imagePoints.size(), patternSize, patternScale);
            Calib3d.calibrateCamera(
                    objectPoints, imagePoints, imageSize, cameraMatrix,
                    distortionCoefficients, rotationMatrices, translationVectors
            );

            status = STATUS_SUCCESS;
        } catch (Exception e) {
            status = STATUS_ERROR;
            System.err.println(e);
        } finally {
            imagePoints.clear();
            cachedImagePoints.release();
        }
    }
}
