package ai.flow.modeld.transforms;

import ai.flow.common.Path;
import org.jocl.*;

import java.nio.ByteBuffer;

import static ai.flow.common.utils.readFile;
import static ai.flow.modeld.CommonModelF3.*;
import static org.jocl.CL.*;

public class LoadYUVCL {
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel load_ys_kernel;
    private cl_kernel load_uv_kernel;
    private cl_kernel copy_kernel;

    public int[] global_out_off = new int[]{0};

    public cl_mem net_input_cl;

    public long[] copy_work_size = new long[]{((MODEL_WIDTH * MODEL_HEIGHT) + ((MODEL_WIDTH/2) * (MODEL_HEIGHT/2))*2)/8};
    public long[] loadys_work_size = new long[]{(MODEL_WIDTH * MODEL_HEIGHT)/8};
    public long[] loaduv_work_size = new long[]{(MODEL_WIDTH/2 * MODEL_HEIGHT/2)/8};

    public LoadYUVCL(cl_context context, cl_command_queue commandQueue){
        this.context = context;
        this.commandQueue = commandQueue;

        String programSource = readFile(Path.internal("selfdrive/assets/clkernels/loadyuv.cl"));
        program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        String args = String.format("-cl-fast-relaxed-math -cl-denorms-are-zero -DTRANSFORMED_WIDTH=%d -DTRANSFORMED_HEIGHT=%d", MODEL_WIDTH, MODEL_HEIGHT);
        clBuildProgram(program, 0, null, args, null, null);

        // Create the kernel
        load_ys_kernel = clCreateKernel(program, "loadys", null);
        load_uv_kernel = clCreateKernel(program, "loaduv", null);
        copy_kernel = clCreateKernel(program, "copy", null);

        net_input_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_float*MODEL_FRAME_SIZE*2, null, null);
    }

    public void run(cl_mem y_cl, cl_mem u_cl, cl_mem v_cl, boolean do_shift){
        global_out_off[0] = 0;
        if (do_shift) {
            // shift the image in slot 1 to slot 0, then place the new image in slot 1
            global_out_off[0] += (MODEL_WIDTH * MODEL_HEIGHT) + ((MODEL_WIDTH / 2) * (MODEL_HEIGHT / 2)) * 2;
            clSetKernelArg(copy_kernel, 0, Sizeof.cl_mem, Pointer.to(net_input_cl));
            clSetKernelArg(copy_kernel, 1, Sizeof.cl_int, Pointer.to(global_out_off));
            clEnqueueNDRangeKernel(commandQueue, copy_kernel, 1, null,
                    copy_work_size, null, 0, null, null);
        }

        clSetKernelArg(load_ys_kernel, 0, Sizeof.cl_mem, Pointer.to(y_cl));
        clSetKernelArg(load_ys_kernel, 1, Sizeof.cl_mem, Pointer.to(net_input_cl));
        clSetKernelArg(load_ys_kernel, 2, Sizeof.cl_int, Pointer.to(global_out_off));

        clEnqueueNDRangeKernel(commandQueue, load_ys_kernel, 1, null,
                loadys_work_size, null, 0, null, null);

        global_out_off[0] += (MODEL_WIDTH*MODEL_HEIGHT);
        clSetKernelArg(load_uv_kernel, 0, Sizeof.cl_mem, Pointer.to(u_cl));
        clSetKernelArg(load_uv_kernel, 1, Sizeof.cl_mem, Pointer.to(net_input_cl));
        clSetKernelArg(load_uv_kernel, 2, Sizeof.cl_int, Pointer.to(global_out_off));

        clEnqueueNDRangeKernel(commandQueue, load_uv_kernel, 1, null,
                loaduv_work_size, null, 0, null, null);

        global_out_off[0] += (MODEL_WIDTH/2 * MODEL_HEIGHT/2);
        clSetKernelArg(load_uv_kernel, 0, Sizeof.cl_mem, Pointer.to(v_cl));
        clSetKernelArg(load_uv_kernel, 1, Sizeof.cl_mem, Pointer.to(net_input_cl));
        clSetKernelArg(load_uv_kernel, 2, Sizeof.cl_int, Pointer.to(global_out_off));

        clEnqueueNDRangeKernel(commandQueue, load_uv_kernel, 1, null,
                loaduv_work_size, null, 0, null, null);
    }

    public void read_buffer(ByteBuffer buffer){
        assert (buffer.remaining() == Sizeof.cl_float*MODEL_FRAME_SIZE*2);
        clEnqueueReadBuffer(commandQueue, net_input_cl, CL_TRUE, 0, 2*MODEL_FRAME_SIZE * Sizeof.cl_float, Pointer.to(buffer), 0, null, null);
    }

    public void dispose(){
        clReleaseKernel(load_uv_kernel);
        clReleaseKernel(load_ys_kernel);
        clReleaseKernel(copy_kernel);
        clReleaseProgram(program);
    }
}
