package net.q1cc.cfs.visual.jairport;

/**
 * This file was edited from its original by cfstras
 */

import java.util.HashMap;
import java.util.Map;

public class RaopSessionManager {
  private static Map<String, RaopSession> map = new HashMap<String, RaopSession>();

  public static RaopSession getSession(String sessionId) {
    return map.get(sessionId);
  }

  public static void addSession(String sessionId, RaopSession session) {
    map.put(sessionId, session);
  }

  public static void shutdownSession(String sessionId) {
    System.out.println("Shutdown Session");
    RaopSession session = getSession(sessionId);
    if(session == null) {
      System.out.println("Session not found " + sessionId);
      return;
    }
    session.getServer().interrupt();
    session.getServer().shutdownAudio();
    map.remove(session);
  }
}
