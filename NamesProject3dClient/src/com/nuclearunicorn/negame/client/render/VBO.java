/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.client.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.ARBVertexBufferObject;

import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 *
 * @author Red
 */
public class VBO {

    

    static int totalNumberOfAxis = 3;
    static int floatSize = 4;
    static int vertexPositionAttributeSize = ((3+3+2) * floatSize);   //3 position coord + 3 normal + 2 texture coord
    int vertexIndexSize = 4;
    int totalVertecies = 4;

    public static final int vboFramebufferSize = 2;
    public static volatile int framebufferId = 0;

    static int[] vboidData = new int[vboFramebufferSize];
    static int[] vboidIndex = new int[vboFramebufferSize];

    public static int[] VBO_buffer_size = new int[vboFramebufferSize];

    public static volatile ByteBuffer[] vertexPositionAttributes    = new ByteBuffer[vboFramebufferSize];
    public static volatile ByteBuffer[] vertexIndecies              = new ByteBuffer[vboFramebufferSize];

    public int vertex_index = 0;

    //32*32*32 * 4 * 6
    public static final  int VBO_max_buffer_size = 32000 * 4 * 6;

    public static boolean vbo_invalidate = false;

    public Texture texture;

    public static int get_framebuffer_inactive(){
        int id = framebufferId + 1;
        if (id >= vboFramebufferSize){
            id = 0;
        }

        return id;
        //return framebufferId;
    }

    public ByteBuffer get_vpa(){
        int fbi = get_framebuffer_inactive();
        return vertexPositionAttributes[fbi];
    }

    public ByteBuffer get_vi(){
        int fbi = get_framebuffer_inactive();
        return vertexIndecies[fbi];
    }


    public void init(){
        try {
            texture = TextureLoader.getTexture("PNG", new FileInputStream("Data/terrain.png"));
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static int createVBOID() {
        return ARBVertexBufferObject.glGenBuffersARB();
    }

  
    public static void process_bufferData(int vbo_id, int TYPE, ByteBuffer buffer){
        ARBVertexBufferObject.glBindBufferARB(TYPE, vbo_id);
        //NLTimer.pop("glBindBufferARB");
        
        ARBVertexBufferObject.glBufferDataARB(TYPE, buffer, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
        //NLTimer.pop("glBufferDataARB");

        //7 ms flickering there
    }

    //--------------------------------------------------------------------------
    //  Обновить vbo из нового буфера
    //--------------------------------------------------------------------------

    /*ByteBuffer buff_data    = ByteBuffer.allocate(vertexPositionAttributeSize *VBO_max_buffer_size);
    ByteBuffer buff_index   = ByteBuffer.allocate(vertexIndexSize * VBO_max_buffer_size);*/

    public void update_vbo_buffer(){

        int ifb_id = get_framebuffer_inactive();
        
        if (vbo_invalidate == false){
            return;
        }else{
            System.out.println("updating invalidated VBO");
        }
        
        //if new buffer generated

        if (vboidData[ifb_id] == 0){
            vboidData[ifb_id]  =   createVBOID();
        }
        if (vboidIndex[ifb_id] == 0){
            vboidIndex[ifb_id]  =   createVBOID();
        }

        //NLTimer.push();
        process_bufferData(vboidIndex[ifb_id], ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, vertexIndecies[ifb_id]);
        //NLTimer.pop("streamed vboidIndex");

        process_bufferData(vboidData[ifb_id],  ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vertexPositionAttributes[ifb_id]);
        //NLTimer.pop("streamed vboidData");



        framebufferId = get_framebuffer_inactive();    //<<< swap buffer
        vbo_invalidate = false;
        //Render.vbo_locked = false;
    }

    //rebuild VBO data based on current visible area

    public void rebuild_buffer( ){

        int ifb_id = get_framebuffer_inactive();

        vertexPositionAttributes[ifb_id]   = ByteBuffer.
                allocateDirect(vertexPositionAttributeSize * VBO_max_buffer_size).
                order(ByteOrder.nativeOrder());
        vertexIndecies[ifb_id]             = ByteBuffer.
                allocateDirect(vertexIndexSize * VBO_max_buffer_size).
                order(ByteOrder.nativeOrder());
    }

    public void build_chunks_all(){
        
        VBO_buffer_size[get_framebuffer_inactive()] = 0;

        /*for (int x = Map.cluster_x; x< Map.cluster_x+Map.cluster_size; x++)
        for (int y = Map.cluster_y; y< Map.cluster_y+Map.cluster_size; y++)
        for (int z = Map.cluster_z; z< Map.cluster_z+Map.cluster_size; z++)
        {
            build_chunk(x,y,z);
        }*/
    }


    public void build_chunk(int chunk_x, int chunk_y, int chunk_z){
        /*Tile __tile = null;
        Tile __nb = null;

        Map __map = World.game_map;
        int CS = Map.__CHUNK_SIZE;

        int max_x = (Map.cluster_x+Map.cluster_size)*CS;
        int max_y = (Map.cluster_y+Map.cluster_size)*CS;
        int max_z = (Map.cluster_z+Map.cluster_size)*CS;

        int min_x = (Map.cluster_x)*CS;
        int min_y = (Map.cluster_y)*CS;
        int min_z = (Map.cluster_z)*CS;

        for (int x=chunk_x*CS; x < (chunk_x+1)*CS; x++)
        for (int y=chunk_y*CS; y < (chunk_y+1)*CS; y++)
        for (int z=chunk_z*CS; z < (chunk_z+1)*CS; z++)
        {
                    __tile = __map.get_tile(x, y, z);
                    if (!Tile.empty(__tile))
                    {

                        //f k l r t b
                        //------------------------ Z ---------------------------
                        if ( (z == min_z) || (!__map.empty(x,y,z-1))){
                            __tile.kv = false;
                        }
                        if ( (z == max_z-1) || (!__map.empty(x,y,z+1))){
                            __tile.fv = false;
                        }
                        
                        //------------------------ X ---------------------------
                        if ( (x == min_x) || (!__map.empty(x-1,y,z))){
                            __tile.lv = false;
                        }
                        if ( (x == max_x-1) || (!__map.empty(x+1,y,z))){
                            __tile.rv = false;
                        }

                        //------------------------ Y ---------------------------
                        if ( (y == min_y) || (!__map.empty(x,y-1,z))){
                            __tile.bv = false;
                        }
                        if ( (y == max_y-1) ||(!__map.empty(x,y+1,z))){
                            __tile.tv = false;
                        }


                        voxel_render.set_origin(x, y, z);
                        voxel_render.textureTileId = __tile.tile_type;
                        voxel_render.build_vbo(this, __tile);
                    }
        }*/
    }

    public void render(){
        if (texture != null){
            texture.bind();
        }
        
        if (vboidData[framebufferId] == 0 || vboidIndex[framebufferId] == 0){
            return;
        }


        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

        ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vboidData[framebufferId]);
        ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, vboidIndex[framebufferId]);

        int stride = (3+3+2) * 4;   //3 vertex + 2 texture

        int offset = 0 * 4;
        GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, offset);

        offset = 3 * 4;
        GL11.glNormalPointer(GL11.GL_FLOAT, stride, offset);

        // 3 vertex coord + 3 normal coord * size of float
        offset = (3+3) * 4;
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, offset);
        GL11.glDrawElements(GL11.GL_QUADS, VBO_buffer_size[framebufferId], GL11.GL_UNSIGNED_INT,0);

        ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, 0);
	    ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

        
       
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    }

}
