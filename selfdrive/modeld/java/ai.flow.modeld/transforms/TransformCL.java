package ai.flow.modeld.transforms;

import ai.flow.common.Path;
import org.jocl.*;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.nio.ByteBuffer;

import static ai.flow.common.utils.readFile;
import static ai.flow.modeld.CommonModelF3.*;
import static org.jocl.CL.*;

public class TransformCL {
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel kernel;

    public cl_mem y_cl, u_cl, v_cl, m_y_cl, m_uv_cl;
    public int[] in_y_width = new int[1];
    public int[] in_y_height = new int[1];
    public int[] in_y_px_stride = new int[1];
    public int[] in_uv_width = new int[1];
    public int[] in_uv_height = new int[1];
    public int[] in_uv_px_stride = new int[1];
    public int[] in_u_offset = new int[1];
    public int[] in_v_offset = new int[1];
    public int[] in_stride = new int[1];
    public int[] out_y_width = new int[]{MODEL_WIDTH};
    public int[] out_y_height = new int[]{MODEL_HEIGHT};
    public int[] out_uv_width = new int[]{MODEL_WIDTH/2};
    public int[] out_uv_height =new int[]{MODEL_HEIGHT/2};
    public int[] zero = new int[]{0};
    public long[] work_size_y = new long[]{out_y_width[0], out_y_height[0]};
    public long[] work_size_uv = new long[]{out_uv_width[0], out_uv_height[0]};

    public TransformCL(cl_context context, cl_command_queue commandQueue, int y_width, int y_height, int y_px_stride, int uv_width,
                       int uv_height, int uv_px_stride, int u_offset, int v_offset, int stride){

        this.context = context;
        this.commandQueue = commandQueue;

        in_y_width[0] = y_width;
        in_y_height[0] = y_height;
        in_y_px_stride[0] = y_px_stride;
        in_uv_width[0] = uv_width;
        in_uv_height[0] = uv_height;
        in_uv_px_stride[0] = uv_px_stride;
        in_u_offset[0] = u_offset;
        in_v_offset[0] = v_offset;
        in_stride[0] = stride;

        String programSource = readFile(Path.internal("selfdrive/assets/clkernels/transform.cl"));
        program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(program, "warpPerspective", null);

        y_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, MODEL_WIDTH * MODEL_HEIGHT, null, null);
        u_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, (MODEL_WIDTH / 2) * (MODEL_HEIGHT / 2), null, null);
        v_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, (MODEL_WIDTH / 2) * (MODEL_HEIGHT / 2), null, null);
        m_y_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_float * 3 * 3, null, null);
        m_uv_cl = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_float * 3 * 3, null, null);
    }

    public void run(cl_mem yuv_cl, INDArray M){
        INDArray M_uv = transform_scale_buffer(M, 0.5f);

        clEnqueueWriteBuffer(commandQueue, m_y_cl, CL_TRUE, 0, 3*3*Sizeof.cl_float, Pointer.to(M.data().asNio()), 0, null, null);
        clEnqueueWriteBuffer(commandQueue, m_uv_cl, CL_TRUE, 0, 3*3*Sizeof.cl_float, Pointer.to(M_uv.data().asNio()), 0, null, null);

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(yuv_cl));  // src
        clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(in_stride));  // src_row_stride
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(in_y_px_stride));  // src_px_stride
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(zero));  // src_offset
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(in_y_height));  // src_rows
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(in_y_width));  // src_cols
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(y_cl));  // dst
        clSetKernelArg(kernel, 7, Sizeof.cl_int, Pointer.to(out_y_width));  // dst_row_stride
        clSetKernelArg(kernel, 8, Sizeof.cl_int, Pointer.to(zero));  // dst_offset
        clSetKernelArg(kernel, 9, Sizeof.cl_int, Pointer.to(out_y_height));  // dst_rows
        clSetKernelArg(kernel, 10, Sizeof.cl_int, Pointer.to(out_y_width));  // dst_cols
        clSetKernelArg(kernel, 11, Sizeof.cl_mem, Pointer.to(m_y_cl));  // M

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                work_size_y, null, 0, null, null);

        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(in_uv_px_stride));  // src_px_stride
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(in_u_offset));  // src_offset
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(in_uv_height));  // src_rows
        clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(in_uv_width));  // src_cols
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(u_cl));  // dst
        clSetKernelArg(kernel, 7, Sizeof.cl_int, Pointer.to(out_uv_width));  // dst_row_stride
        clSetKernelArg(kernel, 8, Sizeof.cl_int, Pointer.to(zero));  // dst_offset
        clSetKernelArg(kernel, 9, Sizeof.cl_int, Pointer.to(out_uv_height));  // dst_rows
        clSetKernelArg(kernel, 10, Sizeof.cl_int, Pointer.to(out_uv_width));  // dst_cols
        clSetKernelArg(kernel, 11, Sizeof.cl_mem, Pointer.to(m_uv_cl));  // M

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                work_size_uv, null, 0, null, null);

        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(in_v_offset));  // src_offset
        clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(v_cl));  // dst

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                work_size_uv, null, 0, null, null);
    }

    public void read_buffer(ByteBuffer buffer){
        assert (buffer.remaining() == MODEL_WIDTH*MODEL_HEIGHT);
        clEnqueueReadBuffer(commandQueue, y_cl, CL_TRUE, 0, MODEL_WIDTH*MODEL_HEIGHT, Pointer.to(buffer), 0, null, null);
    }

    public void dispose(){
        clReleaseKernel(kernel);
        clReleaseProgram(program);
    }
}
