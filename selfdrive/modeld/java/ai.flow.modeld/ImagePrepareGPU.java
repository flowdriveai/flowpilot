package ai.flow.modeld;

import ai.flow.common.transformations.RGB2YUV;
import ai.flow.modeld.transforms.LoadYUVCL;
import ai.flow.modeld.transforms.TransformCL;
import org.jocl.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.ByteBuffer;

import static org.jocl.CL.*;

public class ImagePrepareGPU implements ImagePrepare{
    private cl_context context;
    private cl_command_queue commandQueue;

    RGB2YUV rgb2yuv = null;
    TransformCL transformCL;
    LoadYUVCL loadYUVCL;
    cl_mem yuv_cl;

    public int H;
    public int W;
    public int MODEL_WIDTH = 512;
    public int MODEL_HEIGHT = 256;
    public int MODEL_FRAME_SIZE = MODEL_WIDTH * MODEL_HEIGHT * 3 / 2;
    public int buf_size = MODEL_FRAME_SIZE * 2;
    public final INDArray netInputBuff = Nd4j.zeros(1, 12, MODEL_HEIGHT/2, MODEL_WIDTH/2);

    public boolean rgb;

    public ImagePrepareGPU(int W, int H, boolean rgb, int y_width, int y_height, int y_px_stride, int uv_width,
                           int uv_height, int uv_px_stride, int u_offset, int v_offset, int stride) {
        this.H = H;
        this.W = W;
        this.rgb = rgb;

        initCL();

        if (rgb)
            rgb2yuv = new RGB2YUV(context, this.commandQueue, H, W);
        else
            yuv_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, H*W*3/2, null, null);
        transformCL = new TransformCL(context, commandQueue, y_width, y_height, y_px_stride, uv_width, uv_height, uv_px_stride, u_offset, v_offset, stride);
        loadYUVCL = new LoadYUVCL(context, commandQueue);
    }

    public INDArray prepare(ByteBuffer imgBuffer, INDArray transform){
        cl_mem yuv_cl;
        if (rgb) {
            rgb2yuv.run(imgBuffer);
            yuv_cl = rgb2yuv.yuv_cl;
        }
        else {
            yuv_cl = this.yuv_cl;
            clEnqueueWriteBuffer(commandQueue, yuv_cl, CL_TRUE, 0, H*W*3/2, Pointer.to(imgBuffer), 0, null, null);
        }
        transformCL.run(yuv_cl, transform);
        loadYUVCL.run(transformCL.y_cl, transformCL.u_cl, transformCL.v_cl, true);
        clFinish(commandQueue);

        loadYUVCL.read_buffer(netInputBuff.data().asNio());
        return netInputBuff;
    }

    public void initCL(){
        final int platformIndex = 0;
        final int deviceIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_GPU;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue = clCreateCommandQueue(context, device, 0, null);
    }

    public void dispose(){
        rgb2yuv.dispose();
        loadYUVCL.dispose();
        transformCL.dispose();
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}
