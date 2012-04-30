/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual;

import java.util.LinkedList;

/**
 *
 * @author claus
 */
public class OGLMain extends Thread {
    
    //the Animation queue
    LinkedList<Animation> animQueue;
    
    OGLMain() {
        //init lists etc
        animQueue = new LinkedList<Animation>();
        
        
        
    }
    
    @Override public void run() {
        
    }
    
    void initDisplay() {
        
    }
    
    void initGL() {
        
    }
    
    void drawFrame() {
        
    }
    
    
    void triggerAnimation(Animation animation) {
        animQueue.addLast(animation);
    }
    
    
    enum Animation{
        startSong,
        endSong,
        startup,
        connected,
        idle,
        disconnected
        //special easter egg?
    }
}
