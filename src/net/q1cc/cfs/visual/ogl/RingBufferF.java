/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual.ogl;

/**
 *
 * @author claus
 */
public class RingBufferF {
    int size;
    float[] buf;
    /*
     * Points to the first value
     */
    int startpos;
    /**
     * points to the last value
     */
    int endpos;
    
    public RingBufferF(int size) {
        this.size=size;
        if(size<=0) {
            System.out.println("Error: Ringbuffer size cannot be "+size
                    +". Next time you try this, I will throw you a NPE in your face.");
            return;
        }
        buf = new float[size];
        startpos = 0;
        endpos = -1;
    }
    
    public float peek(int pos) {
        return buf[(startpos+pos)%size];
    }
    public float peek() {
        return buf[endpos];
    }
    public float peekFirst() {
        return buf[startpos];
    }
    public void push(float val) {
        buf[(endpos+1)%size]=val;
        endpos = (endpos+1)%size;
        if(endpos == startpos) {
            startpos = (endpos+1)%size;
        }
    }
    public float pop() {
        if(endpos>-1) {
            endpos--;
            return buf[endpos+1];
        }
        return buf[endpos];
    }
    
    public int size() {
        return endpos-startpos+1;
    }
}
