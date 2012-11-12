package net.q1cc.cfs.visual;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.JmmDNSImpl;
import javax.jmdns.impl.NetworkTopologyEventImpl;
import java.io.IOException;
import java.net.InetAddress;
import net.q1cc.cfs.visual.jairport.Configuration;
import net.q1cc.cfs.visual.jairport.RtspServer;
import net.q1cc.cfs.visual.jairport.Utils;

public class JAirPort {
  private static JmmDNS dns;
  private static volatile Thread serverThread;
  public static Visuals visuals;

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new ShutdownThread());

    String hwAddr = Utils.byteAddrToString(Configuration.getHardwareAddress());
    String host = Configuration.getHostName();
    String name = hwAddr + "@visual: " + host;
    int port = Configuration.getPort();
    
    // Announce Raop Service
    ServiceInfo info = ServiceInfo.create(name + "._raop._tcp.local", name, port, "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 sr=44100 pw=false vn=3 txtvers=1");
    
    dns = JmmDNS.Factory.getInstance();
    ((JmmDNSImpl)dns).inetAddressAdded(new NetworkTopologyEventImpl(JmDNS.create(InetAddress.getByName("localhost")), InetAddress.getByName("localhost")));
    
    Thread.sleep(1000); // If this isn't done the Announcement sometimes doesn't go out on the local interface
    
    dns.registerService(info);
    System.out.println("Service registered");
    
     //now init OGL
    visuals = new Visuals();
    visuals.start();
    
    // Start RtspServer
    RtspServer server = new RtspServer(port);
    serverThread = new Thread(server, "RtspServer");
    serverThread.setDaemon(false);
    serverThread.start();
    
    serverThread.join();
    visuals.run=false;
    visuals.interrupt();
  }

  public static class ShutdownThread extends Thread {
    @Override
    public void run() {
      System.out.println("Shutting down...");
      if(serverThread != null) {
        serverThread.interrupt();
      }

      if (dns != null) {
        dns.unregisterAllServices();
        try {
          dns.close();
        } catch (IOException e) {
          // ignore
        }
        dns = null;
      }
    }
  }
}
