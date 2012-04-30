package net.q1cc.cfs.visual.jairport;

/**
 * This file was edited from its original by cfstras
 */
import com.beatofthedrum.alacdecoder.AlacDecodeUtils;
import com.beatofthedrum.alacdecoder.AlacFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import net.q1cc.cfs.visual.JAirPort;

public class RaopServer extends Thread {

    private RaopSession session;
    private int port;
    private DatagramSocket receiveSocket;
    private Cipher aesCipher;
    private SecretKeySpec secretKey;
    private IvParameterSpec keySpec;
    private SocketAddress clientSocketAddress;
    private AlacFile alac;
    private SourceDataLine line;
    private int[] outbuffer;
    private byte[] outbufferBytes;
    public boolean run = true;

    public RaopServer(RaopSession session) {
        super("RaopServer " + session.getId());

        this.port = session.getControlPort() - 1;
        this.session = session;

        try {
            this.aesCipher = Cipher.getInstance("AES/CBC/NOPADDING");
            receiveSocket = new DatagramSocket(port);

            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        secretKey = new SecretKeySpec(session.getAesKey(), "AES");
        secretKey = new SecretKeySpec(session.getAesKey(), "AES");
        keySpec = new IvParameterSpec(session.getAesIv());

        int frameSize = session.getFormat().getFrameSize();
        alac = AlacDecodeUtils.create_alac(session.getFormat().getSampleSize(), 2);
        alac.setSetinfo_max_samples_per_frame(frameSize);
        alac.setSetinfo_rice_historymult(session.getFormat().getRiceHistoryMult());
        alac.setSetinfo_rice_initialhistory(session.getFormat().getRiceInitialHistory());
        alac.setSetinfo_rice_kmodifier(session.getFormat().getRiceKModifier());
        alac.setSetinfo_sample_size(session.getFormat().getSampleSize());

        outbuffer = new int[frameSize * 4];
        outbufferBytes = new byte[outbuffer.length * 2];
    }

    public void requestRtpResend(int first, int last) throws IOException {
        if (first > last) {
            System.out.println(first + " > " + last);
            return;
        }

        int len = last - first + 1;
        byte[] b = new byte[]{(byte) 0x80, (byte) (0x55 | 0x80), 0x01, 0x00, (byte) ((first & 0xFF00) >> 8), (byte) (first & 0xFF), (byte) ((len & 0xFF00) >> 8), (byte) (len & 0xFF)};
        DatagramPacket packet = new DatagramPacket(b, 0, b.length, clientSocketAddress);
        receiveSocket.send(packet);
    }
    private int lastSeqNo = 0;

    private int convertSampleBufferToByteBuffer(int[] sampleBuffer, int len, byte[] outbuffer) {
        int j = 0;
        for (int i = 0; i < len; ++i) {
            int sample = sampleBuffer[i];
            outbuffer[j++] = (byte) (sample >> 8);
            outbuffer[j++] = (byte) sample;
        }
        return j;
    }
    int lastlen;
    byte[] lastbuf;

    private void putDataInBuffer(int seqNo, byte[] data, int offset, int len) {
        if (lastlen < len) {
            lastbuf = new byte[len];
            System.out.println("bigger buffer size " + len +" > "+lastlen);
            lastlen = len;
        }//else System.out.println("same/smaller buffer size "+len+" < "+lastlen);
        decryptAes(data, offset, len, lastbuf);
        int samplesInBytes = AlacDecodeUtils.decode_frame(alac, lastbuf, outbuffer, outbuffer.length);
        assert (samplesInBytes == session.getFormat().getFrameSize() * 4);

        int lenBytes = convertSampleBufferToByteBuffer(outbuffer, samplesInBytes >> 1, outbufferBytes);
        line.write(outbufferBytes, 0, lenBytes);
        line.flush();
        //if(seqNo + 1 < lastSeqNo) {
        //  System.out.println("bla " + seqNo  + " " + lastSeqNo);
        //}
        lastSeqNo = seqNo;
    }

    @Override
    public void run() {
        
        try {
            line = JAirPort.visuals.setServer(this, new AudioFormat(session.getFormat().getSampleRate(), session.getFormat().getSampleSize(), 2, true, true));
            line.start();
            line.open();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }
        
        byte[] b = new byte[2048];
        DatagramPacket packet = new DatagramPacket(b, b.length);
        while (!Thread.interrupted() && run) {
            try {
                receiveSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                this.interrupt();
                continue;
            }
            if (clientSocketAddress == null) {
                clientSocketAddress = packet.getSocketAddress();
            }

            int offset = packet.getOffset();
            int length = packet.getLength();
            byte[] data = packet.getData();
            byte type = (byte) (data[offset + 1] & ~0x80);
            if (type == 0x60 || type == 0x56) { // audio data / resend
                if (type == 0x56) {
                    offset = offset + 4;
                    length = length - 4;
                }
                int seqno = (data[offset + 2] << 8) | data[offset + 3];
                putDataInBuffer(seqno, data, offset + 12, length - 12);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void shutdownAudio() {
        line.drain();
        line.close();
        line.stop();
        run = false;
        line = null;
    }

    private int decryptAes(byte[] inbuf, int offset, int len, byte[] outbuffer) {
        int decodeLen = len - (len % 16);
        try {
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey, keySpec);
            int ret = aesCipher.doFinal(inbuf, offset, decodeLen, outbuffer);
            System.arraycopy(inbuf, offset + decodeLen, outbuffer, decodeLen, len % 16);
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
