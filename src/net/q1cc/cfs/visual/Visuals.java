/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import net.q1cc.cfs.visual.jairport.RaopServer;
import net.q1cc.cfs.visual.ogl.RingBufferF;
import net.q1cc.cfs.visual.ogl.RingBufferI;
import net.q1cc.cfs.visual.ogl.SoundState;

/**
 *
 * @author claus
 */
public class Visuals extends Thread {
    
    //constants
    final static int waitBeforeIdle = 3000;
    
    private byte[] buffer;
    private int buflen;
    private boolean bufferFinished=true;
    
    LineWrapper lw;
    private boolean waitForData=false;
    boolean run=true;
    long waitingSince;
    
    int bytesPerSample;
    float samplesPerSecond;
    int channels;
    boolean isBigEndian;
    int maxLevel;
    
    SoundState soundState;
    
    OGLMain ogl;
    AudioFormat audioFormat;
    
    public Visuals() {
        super("VisualWorker");
        waitingSince = System.currentTimeMillis();
        buffer=new byte[1];
    }
    
    public SourceDataLine setServer(RaopServer s,AudioFormat af) throws LineUnavailableException {
        //TODO check if this was already called
        audioFormat = af;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
        SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
        //System.out.println(sdl);
        lw = new LineWrapper(sdl,this);
        
        System.out.println(info.toString()+"\n"+af.toString());
        
        //do some prep stuff
        bytesPerSample = af.getSampleSizeInBits() >> 3;
        samplesPerSecond = af.getSampleRate();
        channels = af.getChannels();
        isBigEndian = af.isBigEndian();
        maxLevel = 1 << (af.getSampleSizeInBits()); // TODO if signed integer
        if(bytesPerSample!=2) {
            System.out.println("Warning: values will be rubbish because i expect 16-bit values as opposed to "+af.getSampleSizeInBits());
        }
        
        //TODO signed
        
        //setup SoundState
        soundState = new SoundState(info);
        soundState.lastvolume = new RingBufferF(100);
        soundState.lastvolume1 = new RingBufferF(10);
        soundState.lastvolume5 = new RingBufferF(10);
        soundState.lastvolume10 = new RingBufferF(10);
        return lw;
    }
    
    @Override public void run() {
        //init OpenGL
        ogl = new OGLMain();
        ogl.start();
        
        //run startup animation
        ogl.triggerAnimation(OGLMain.Animation.startup);
        
        //start waiting for data
        while(run) {
            if(waitForData) {
                if(!bufferFinished) { // means we can go now
                    processBuffer();
                    bufferFinished=true;
                } else {
                    try { sleep(100); } catch (InterruptedException ex) {}
                }
            } else {
                if(waitingSince+waitBeforeIdle < System.currentTimeMillis() ) {
                    waitingSince = Integer.MAX_VALUE-waitBeforeIdle; // no triggering anymore
                    ogl.triggerAnimation(OGLMain.Animation.idle);
                }
                try { sleep(400); } catch (InterruptedException ex) {}
            }
        }
    }
    
    void sdlStarted() {
        waitForData=true;
        waitingSince = Integer.MAX_VALUE-waitBeforeIdle;
        interrupt();
        ogl.triggerAnimation(OGLMain.Animation.connected);
    }
    void sdlStopped() {
        waitForData=false;
        interrupt();
        waitingSince = System.currentTimeMillis();
        ogl.triggerAnimation(OGLMain.Animation.disconnected);
        
    }

    private void processBuffer() {
        //TODO fancy FFTs and stuff
        
        doVolume();
        
    }
    
    private void doVolume() {
        /*
         * http://imgur.com/sslIe
         * byte array: 16 bit werte, abwechselnd links und rechts
         * info anschauen!
         * 
         */
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0,buflen);
        bb.order(isBigEndian?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN);
        ShortBuffer ib = bb.asShortBuffer();
        int size = buflen>>1; // TODO if moar bits
        
        float acc=0;
        short v=0;
        for(int i=0;i<size;i++) {
            v=ib.get(i);
            if(v>0) acc+=v;
            else acc-=v;
        }
        
        acc /= size*channels*maxLevel; // durchschnitt berechnen
        //TODO split this into 1/100ths of a second
        soundState.volume=acc;
        soundState.lastvolume.push(acc);
        //TODO lastvolume1, 5 & 10
        
        //System.out.println(lw.sdl.getLevel()+ "  "+lw.sdl.getMicrosecondPosition());
        //System.out.println(s.toString());
    }
    
    private class LineWrapper implements SourceDataLine {
        SourceDataLine sdl;
        Visuals v;
        public LineWrapper(SourceDataLine sdl, Visuals parent) {
            v=parent;
            this.sdl=sdl;
        }
        @Override
        public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
            sdl.open(format, bufferSize);
        }

        @Override
        public void open(AudioFormat format) throws LineUnavailableException {
            sdl.open(format);
        }

        @Override
        public int write(byte[] b, int off, int len) {
            if(v.bufferFinished) {
                if(len>v.buffer.length){
                    System.out.println("increasing buffer size from "+v.buffer.length+" to "+len);
                    v.buffer = new byte[len];
                }
                System.arraycopy(b, off, v.buffer, 0, len);
                v.buflen=len;
                v.bufferFinished=false;
                //interrupt?
            }
            return sdl.write(b, off, len);
        }

        @Override
        public void drain() {
            sdl.drain();
        }

        @Override
        public void flush() {
            sdl.flush();
        }

        @Override
        public void start() {
            sdl.start();
            Visuals.this.sdlStarted();
        }

        @Override
        public void stop() {
            sdl.stop();
            Visuals.this.sdlStopped();
        }

        @Override
        public boolean isRunning() {
            return sdl.isRunning();
        }

        @Override
        public boolean isActive() {
            return sdl.isActive();
        }

        @Override
        public AudioFormat getFormat() {
            return sdl.getFormat();
        }

        @Override
        public int getBufferSize() {
            return sdl.getBufferSize();
        }

        @Override
        public int available() {
            return sdl.available();
        }

        @Override
        public int getFramePosition() {
            return sdl.getFramePosition();
        }

        @Override
        public long getLongFramePosition() {
            return sdl.getLongFramePosition();
        }

        @Override
        public long getMicrosecondPosition() {
            return sdl.getMicrosecondPosition();
        }

        @Override
        public float getLevel() {
            return sdl.getLevel();
        }

        @Override
        public Info getLineInfo() {
            return (Info) sdl.getLineInfo();
        }

        @Override
        public void open() throws LineUnavailableException {
            sdl.open();
        }

        @Override
        public void close() {
            sdl.close();
        }

        @Override
        public boolean isOpen() {
            return sdl.isOpen();
        }

        @Override
        public Control[] getControls() {
            return sdl.getControls();
        }

        @Override
        public boolean isControlSupported(Type control) {
            return sdl.isControlSupported(control);
        }

        @Override
        public Control getControl(Type control) {
            return sdl.getControl(control);
        }

        @Override
        public void addLineListener(LineListener listener) {
            sdl.addLineListener(listener);
        }

        @Override
        public void removeLineListener(LineListener listener) {
            sdl.addLineListener(listener);
        }
        
    }
}
