package ai.flow.common.transformations;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

import static ai.flow.common.BufferUtils.MatToByteBuffer;

public class YUV2RGB {
    Mat yuv_mat = null;
    Mat matRGB = null;
    ByteBuffer rgbBuffer;
    int w, h, chromaPixelStride, stride;
    public YUV2RGB(int w, int h, int stride, int chromaPixelStride){
        this.w = w;
        this.h = h;
        this.chromaPixelStride = chromaPixelStride;
        this.stride = stride;
        yuv_mat = new Mat(h + h / 2, w, CvType.CV_8UC1);
        matRGB = new Mat(h, w, CvType.CV_8UC3);
        rgbBuffer = MatToByteBuffer(matRGB);
    }

    public Mat run(ByteBuffer yuv){
        // Warning: this only assumes nv12 yuv format.
        Mat yuv_mat = new Mat(h*3/2, w, CvType.CV_8UC1, yuv);
        Imgproc.cvtColor(yuv_mat, matRGB, Imgproc.COLOR_YUV2RGB_NV21);
        yuv_mat.release();
        return matRGB;
    }

    public Mat run(ByteBuffer y, ByteBuffer u, ByteBuffer v) {
        if (chromaPixelStride == 2) { // Chroma channels are interleaved
            ByteBuffer y_plane = y;
            int y_plane_step = stride;
            ByteBuffer uv_plane1 = u;
            int uv_plane1_step = stride;
            ByteBuffer uv_plane2 = v;
            int uv_plane2_step = stride;
            Mat y_mat = new Mat(h, w, CvType.CV_8UC1, y_plane, y_plane_step);
            Mat uv_mat1 = new Mat(h / 2, w / 2, CvType.CV_8UC2, uv_plane1, uv_plane1_step);
            Mat uv_mat2 = new Mat(h / 2, w / 2, CvType.CV_8UC2, uv_plane2, uv_plane2_step);
            long addr_diff = uv_mat2.dataAddr() - uv_mat1.dataAddr();
            if (addr_diff > 0) {
                assert (addr_diff == 1);
                Imgproc.cvtColorTwoPlane(y_mat, uv_mat1, matRGB, Imgproc.COLOR_YUV2BGR_NV12);
            } else {
                assert (addr_diff == -1);
                Imgproc.cvtColorTwoPlane(y_mat, uv_mat2, matRGB, Imgproc.COLOR_YUV2BGR_NV12);
            }
            y_mat.release();
            uv_mat1.release();
            uv_mat2.release();
        } else { // Chroma channels are not interleaved
            byte[] yuv_bytes = new byte[w * (h + h / 2)];
            ByteBuffer y_plane = y;
            ByteBuffer u_plane = u;
            ByteBuffer v_plane = v;

            int yuv_bytes_offset = 0;

            int y_plane_step = stride;
            if (y_plane_step == w) {
                y_plane.get(yuv_bytes, 0, w * h);
                yuv_bytes_offset = w * h;
            } else {
                int padding = y_plane_step - w;
                for (int i = 0; i < h; i++) {
                    y_plane.get(yuv_bytes, yuv_bytes_offset, w);
                    yuv_bytes_offset += w;
                    if (i < h - 1) {
                        y_plane.position(y_plane.position() + padding);
                    }
                }
                assert (yuv_bytes_offset == w * h);
            }

            int chromaRowStride = stride;
            int chromaRowPadding = chromaRowStride - w / 2;

            if (chromaRowPadding == 0) {
                // When the row stride of the chroma channels equals their width, we can copy
                // the entire channels in one go
                u_plane.get(yuv_bytes, yuv_bytes_offset, w * h / 4);
                yuv_bytes_offset += w * h / 4;
                v_plane.get(yuv_bytes, yuv_bytes_offset, w * h / 4);
            } else {
                // When not equal, we need to copy the channels row by row
                for (int i = 0; i < h / 2; i++) {
                    u_plane.get(yuv_bytes, yuv_bytes_offset, w / 2);
                    yuv_bytes_offset += w / 2;
                    if (i < h / 2 - 1) {
                        u_plane.position(u_plane.position() + chromaRowPadding);
                    }
                }
                for (int i = 0; i < h / 2; i++) {
                    v_plane.get(yuv_bytes, yuv_bytes_offset, w / 2);
                    yuv_bytes_offset += w / 2;
                    if (i < h / 2 - 1) {
                        v_plane.position(v_plane.position() + chromaRowPadding);
                    }
                }
            }

            yuv_mat.put(0, 0, yuv_bytes);
            Imgproc.cvtColor(yuv_mat, matRGB, Imgproc.COLOR_YUV2RGB_I420, 3);
        }
        return matRGB;
    }

    public ByteBuffer getRGBBuffer(){
        return rgbBuffer;
    }

    public void dispose(){
        matRGB.release();
        yuv_mat.release();
    }
}
