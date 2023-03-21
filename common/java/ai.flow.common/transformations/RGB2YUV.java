package ai.flow.common.transformations;

import ai.flow.common.Path;
import org.jocl.*;

import java.nio.ByteBuffer;

import static ai.flow.common.utils.readFile;
import static org.jocl.CL.*;

public class RGB2YUV {
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel kernel;

    public cl_mem yuv_cl;
    public cl_mem rgb_cl;
    private long[] work_size;
    private int H, W;

    public RGB2YUV(cl_context context, cl_command_queue commandQueue, int H, int W){
        this.context = context;
        this.commandQueue = commandQueue;
        this.H = H;
        this.W = W;

        if (context == null || commandQueue == null)
            initCL();

        String programSource = readFile(Path.internal("selfdrive/assets/clkernels/rgb_to_nv12.cl"));
        program = clCreateProgramWithSource(this.context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        String args = String.format(" -DHEIGHT=%d -DWIDTH=%d -DRGB_STRIDE=%d -DUV_WIDTH=%d -DUV_HEIGHT=%d -DRGB_SIZE=%d ", H, W, W*3, W/2, H/2, W*H);
        clBuildProgram(program, 0, null, args, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "rgb_to_nv12", null);
        yuv_cl = clCreateBuffer(this.context, CL_MEM_READ_WRITE, H*W*3/2, null, null);
        rgb_cl = clCreateBuffer(this.context, CL_MEM_READ_WRITE, H*W*3, null, null);

        work_size = new long[]{W % 4 == 0 ? W/4:(W + (4 - W % 4)) / 4, H % 4 == 0 ?  H/4:(H + (4 - H % 4)) / 4};
    }

    public void run(ByteBuffer rgb){
        clEnqueueWriteBuffer(commandQueue, rgb_cl, CL_TRUE, 0, H*W*3, Pointer.to(rgb), 0, null, null);

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(rgb_cl));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(yuv_cl));

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                work_size, null, 0, null, null);
    }
    public void read_buffer(ByteBuffer yuv_buffer){
        clEnqueueReadBuffer(commandQueue, yuv_cl, CL_TRUE, 0, H*W*3/2, Pointer.to(yuv_buffer), 0, null, null);
    }

    // TODO: move initCL to common
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
        clReleaseKernel(kernel);
        clReleaseProgram(program);
    }
}