/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual.ogl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.LinkedList;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author claus
 */
public class OGLMain extends Thread {
    
    boolean run = true;
    
    //the Animation queue
    LinkedList<Animation> animQueue;
    SoundState st;
    DisplayMode dm;
    
    int vaoLine;
    int vboLineR;
    int vboLineL;
    FloatBuffer lineBufR;
    FloatBuffer lineBufL;
    
    public OGLMain(SoundState st) {
        //init lists etc
        animQueue = new LinkedList<Animation>();
        this.st=st;
        
    }
    
    @Override public void run() {
        try {
            initDisplay();
            initGL();
            while(run) {
                drawFrame();
                if(Display.isCloseRequested()) run=false;
            }
            Display.destroy();
        } catch (LWJGLException ex) {
            ex.printStackTrace();
        }
    }
    
    void initDisplay() throws LWJGLException {
        DisplayMode[] dms = Display.getAvailableDisplayModes();
        for(DisplayMode d:dms) {
            if(d.getWidth()==800) {
                if(dm!=null) {
                    if(d.getFrequency()>dm.getFrequency()) {
                        dm = d;
                    }
                } else {
                    dm = d;
                }
                
            }
        }
        Display.setDisplayMode(dm);
        Display.setTitle("visual");
        Display.create(new PixelFormat(8,16,0,1));
    }
    
    void initGL() {
        glClearColor(0.0f,0.0f,0.0f,1.0f);
        glEnable(GL_DEPTH_TEST);
        glClearDepth(1.0);
        
        vaoLine = glGenVertexArrays();
        glBindVertexArray(vaoLine);
        vboLineL = glGenBuffers();
        vboLineR = glGenBuffers();
        //glBindBuffer(GL_ARRAY_BUFFER, vboLine);
        //glBufferData(GL_ARRAY_BUFFER, 128*Float.SIZE*2/8, GL_STREAM_DRAW);
        
        //lineBuf = BufferUtils.createFloatBuffer(st.lastvolume.capacity*2);
        lineBufR = BufferUtils.createFloatBuffer(st.sourceBuffer.capacity()/2*2);
        lineBufL = BufferUtils.createFloatBuffer(st.sourceBuffer.capacity()/2*2);
    }
    
    void drawFrame() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glColor3f(1, 1, 1);
        if(lineBufR.capacity()!=st.sourceBuffer.capacity()/2*2) {
            lineBufR = BufferUtils.createFloatBuffer(st.sourceBuffer.capacity()/2*2);
            lineBufL = BufferUtils.createFloatBuffer(st.sourceBuffer.capacity()/2*2);
        }
        lineBufR.rewind();
        lineBufL.rewind();
        //for(int i=0;i<st.lastvolume.capacity;i++) {
        for(int i=1;i<st.sourceBuffer.capacity();i+=2) {
            //lineBuf.put(i/(float)(st.lastvolume.capacity/2) - 1f);
            lineBufR.put(i/(float)(st.sourceBuffer.capacity()/5.0f) - 0.9f);
            //lineBuf.put(st.lastvolume.buf[i]);
            lineBufR.put(st.sourceBuffer.get(i)/(float)Short.MAX_VALUE/2.0f-0.5f);
        }
        for(int i=0;i<st.sourceBuffer.capacity();i+=2) {
            //lineBuf.put(i/(float)(st.lastvolume.capacity/2) - 1f);
            lineBufL.put(i/(float)(st.sourceBuffer.capacity()/5.0f) - 0.9f);
            //lineBuf.put(st.lastvolume.buf[i]);
            lineBufL.put(st.sourceBuffer.get(i)/(float)Short.MAX_VALUE/2.0f+0.5f);
        }
        lineBufR.rewind();
        lineBufL.rewind();
        glBindBuffer(GL_ARRAY_BUFFER,vboLineR);
        glBufferData(GL_ARRAY_BUFFER, lineBufR,GL_STREAM_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER,vboLineL);
        glBufferData(GL_ARRAY_BUFFER, lineBufL,GL_STREAM_DRAW);
        
        glEnableClientState(GL_VERTEX_ARRAY);
        
        glBindBuffer(GL_ARRAY_BUFFER,vboLineR);
        glVertexPointer(2, GL_FLOAT, 0, 0);
        glDrawArrays(GL_LINE_STRIP, 0,st.lastvolume.capacity);
        
        glBindBuffer(GL_ARRAY_BUFFER,vboLineL);
        glVertexPointer(2, GL_FLOAT, 0, 0);
        glDrawArrays(GL_LINE_STRIP, 0,st.lastvolume.capacity);
        
        Display.update();
    }
    
    
    public void triggerAnimation(Animation animation) {
        animQueue.addLast(animation);
    }
    
    
    public enum Animation{
        startSong,
        endSong,
        startup,
        connected,
        idle,
        disconnected
        //special easter egg?
    }
}
