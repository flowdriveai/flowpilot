package ai.flow.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.nio.ByteBuffer;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE_2D;

// TODO: Make it generalized for different yuv formats.
public class NV12Renderer {
    public ShaderProgram shader;
    public Mesh mesh;
    public Texture yTexture, uvTexture;
    public ByteBuffer yBuffer, uvBuffer;
    int W, H;

    public NV12Renderer(int W, int H){
        //Allocate textures
        yTexture = new Texture(W, H, Pixmap.Format.Intensity); //A 8-bit per pixel format
        uvTexture = new Texture(W/2,H/2, Pixmap.Format.LuminanceAlpha); //A 16-bit per pixel format

        String vertexShader =
                "attribute vec4 a_position;                         \n" +
                        "attribute vec2 a_texCoord;                         \n" +
                        "varying vec2 v_texCoord;                           \n" +

                        "void main(){                                       \n" +
                        "   gl_Position = a_position;                       \n" +
                        "   v_texCoord = a_texCoord;                        \n" +
                        "}                                                  \n";

        //fragment shader code; takes Y,U,V values for each pixel and calculates R,G,B colors,
        String fragmentShader =
                "#ifdef GL_ES                                       \n" +
                        "precision lowp float;                             \n" +
                        "#endif                                             \n" +

                        "varying vec2 v_texCoord;                           \n" +
                        "uniform sampler2D y_texture;                       \n" +
                        "uniform sampler2D uv_texture;                      \n" +

                        "void main (void){                                  \n" +
                        "   float r, g, b, y, u, v;                         \n" +
                        "   y = texture2D(y_texture, v_texCoord).r;         \n" +

                        "   u = texture2D(uv_texture, v_texCoord).a - 0.5;  \n" +
                        "   v = texture2D(uv_texture, v_texCoord).r - 0.5;  \n" +

                        "   r = y + 1.13983*v;                              \n" +
                        "   g = y - 0.39465*u - 0.58060*v;                  \n" +
                        "   b = y + 2.03211*u;                              \n" +

                        "   gl_FragColor = vec4(r, g, b, 1.0);              \n" +
                        "}                                                  \n";

        //Create and compile shader
        shader = new ShaderProgram(vertexShader, fragmentShader);

        //Create mesh that we will draw on, it has 4 vertices corresponding to the 4 corners of the screen
        mesh = new Mesh(true, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"));

        //The vertices include the screen coordinates (between -1.0 and 1.0) and texture coordinates (between 0.0 and 1.0)
        float[] vertices = {
                -1.0f,  1.0f,   // Position 0
                0.0f,   0.0f,   // TexCoord 0
                -1.0f,  -1.0f,  // Position 1
                0.0f,   1.0f,   // TexCoord 1
                1.0f,   -1.0f,  // Position 2
                1.0f,   1.0f,   // TexCoord 2
                1.0f,   1.0f,   // Position 3
                1.0f,   0.0f    // TexCoord 3
        };

        //The indices come in trios of vertex indices that describe the triangles of our mesh
        short[] indices = {0, 1, 2, 0, 2, 3};

        //Set vertices and indices to our mesh
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        this.W = W;
        this.H = H;
    }

    public void render(ByteBuffer imgBuffer){
        yBuffer = imgBuffer.slice();
        yBuffer.limit(W*H);
        uvBuffer = imgBuffer.slice();
        uvBuffer.position(W*H);
        uvBuffer.limit(W*H*3/2);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
        uvTexture.bind();
        Gdx.gl.glTexImage2D(GL_TEXTURE_2D, 0, GL20.GL_LUMINANCE_ALPHA, W/2, H/2, 0, GL20.GL_LUMINANCE_ALPHA, GL20.GL_UNSIGNED_BYTE, uvBuffer);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        yTexture.bind();
        Gdx.gl.glTexImage2D(GL_TEXTURE_2D, 0, GL20.GL_LUMINANCE, W, H, 0, GL20.GL_LUMINANCE, GL20.GL_UNSIGNED_BYTE, yBuffer);

        shader.bind();
        shader.setUniformi("y_texture", 0);
        shader.setUniformi("uv_texture", 1);

        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    public void dispose(){
        mesh.dispose();
        shader.dispose();
        uvTexture.dispose();
        yTexture.dispose();
    }
}
