/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import net.q1cc.cfs.visual.jairport.RaopServer;

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
    
    private boolean waitForData=false;
    boolean run=true;
    long waitingSince;
    
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
        LineWrapper lw = new LineWrapper(sdl);
        
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
        
        StringBuilder s = new StringBuilder();
        for(int i=0;i<buflen;i++) {
            s.append(((int)buffer[i]));
            s.append(" ");
        }
        s.append("buffer ends here");
        System.out.println(s.toString());
    }
    
    private class LineWrapper implements SourceDataLine {
        SourceDataLine sdl;
        public LineWrapper(SourceDataLine sdl) {
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
            if(bufferFinished) {
                if(len>buffer.length){
                    System.out.println("increasing buffer size from "+buffer.length+" to "+len);
                    buffer = new byte[len];
                }
                System.arraycopy(b, off, buffer, 0, len);
                buflen=len;
                bufferFinished=false;
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
