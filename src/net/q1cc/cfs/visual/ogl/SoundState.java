/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.q1cc.cfs.visual.ogl;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author claus
 */
public class SoundState {
    SourceDataLine.Info sdlinfo;
    
    /**
     * the volume of the stream throughout the last received sample
     */
    float volume;
    /**
     * the last values of volumes.
     * size is chosen so that it contains at least the last second
     */
    RingBufferI lastvolume;
    /**
     * the volume of the stream throughout the last second
     */
    float volume1;
    /**
     * the last 10 values of lastvolume1
     */
    RingBufferI lastvolume1;
    /**
     * the volume of the stream throughout the last five seconds
     */
    float volume5;
    /**
     * the last 10 values of lastvolume5
     */
    RingBufferI lastvolume5;
    /**
     * the volume of the stream throughout the last ten seconds
     */
    float volume10;
    /**
     * the last 10 values of lastvolume10
     */
    RingBufferI lastvolume10;
    
    public SoundState(SourceDataLine.Info sdlinfo) {
        this.sdlinfo=sdlinfo;
        
    }
    
    
}
