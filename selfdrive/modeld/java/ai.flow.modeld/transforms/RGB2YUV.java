package ai.flow.modeld.transforms;

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

        String programSource = readFile("selfdrive/assets/clkernels/rgb_to_nv12.cl");
        program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        String args = String.format(" -DHEIGHT=%d -DWIDTH=%d -DRGB_STRIDE=%d -DUV_WIDTH=%d -DUV_HEIGHT=%d -DRGB_SIZE=%d ", H, W, W*3, W/2, H/2, W*H);
        clBuildProgram(program, 0, null, args, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "rgb_to_nv12", null);
        yuv_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, H*W*3/2, null, null);
        rgb_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, H*W*3, null, null);

        work_size = new long[]{W/4, H/4};
    }

    public void run(ByteBuffer rgb){
        clEnqueueWriteBuffer(commandQueue, rgb_cl, CL_TRUE, 0, H*W*3, Pointer.to(rgb), 0, null, null);

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(rgb_cl));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(yuv_cl));

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                work_size, null, 0, null, null);
    }
    public void read_buffer(ByteBuffer yuv_buffer){
        assert (yuv_buffer.remaining() == H*W*3/2);
        clEnqueueReadBuffer(commandQueue, yuv_cl, CL_TRUE, 0, H*W*3/2, Pointer.to(yuv_buffer), 0, null, null);
    }

    public void dispose(){
        clReleaseKernel(kernel);
        clReleaseProgram(program);
    }
}