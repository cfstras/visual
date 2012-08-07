/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual.ogl;

import java.nio.ShortBuffer;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author claus
 */
public class SoundState {
    
    public SourceDataLine.Info sdlinfo;
    
    /**
     * the volume of the stream throughout the last received 441 samples
     */
    public float volume;
    /**
     * the last values of volume.
     * size is chosen so that it contains at least the last second
     */
    public RingBufferF lastvolume;
    /**
     * the volume of the stream throughout the last second
     */
    public float volume1;
    /**
     * the last 128 values of lastvolume1
     */
    public RingBufferF lastvolume1;
    /**
     * the volume of the stream throughout the last five seconds
     */
    public float volume5;
    /**
     * the last 128 values of lastvolume5
     */
    public RingBufferF lastvolume5;
    /**
     * the volume of the stream throughout the last ten seconds
     */
    public float volume10;
    /**
     * the last 128 values of lastvolume10
     */
    public RingBufferF lastvolume10;
    
    public ShortBuffer sourceBuffer;
    public byte[] buffer;
    
    public SoundState(SourceDataLine.Info sdlinfo) {
        this.sdlinfo=sdlinfo;
        
    }
    
    
}
