package com.smipl.lcrecon.util;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active authenticated HTTP sessions, keyed by user id.
 * Enables invalidating a user's other sessions after a password change/reset
 * (VAPT WEB_VUL_03 - active sessions not invalidated after password change).
 *
 * Populated at login ({@code register}) and cleaned when a session is destroyed
 * (via SessionCleanupListener).
 */
public final class SessionRegistry {

    private SessionRegistry() {}

    // userId -> (sessionId -> session)
    private static final Map<Long, Map<String, HttpSession>> SESSIONS = new ConcurrentHashMap<>();

    /** Register an authenticated session for a user (called after successful login). */
    public static void register(long userId, HttpSession session) {
        if (session == null) return;
        SESSIONS.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
    }

    /** Remove a single session from the registry (called on session destroy). */
    public static void unregister(long userId, String sessionId) {
        Map<String, HttpSession> m = SESSIONS.get(userId);
        if (m != null) {
            m.remove(sessionId);
            if (m.isEmpty()) SESSIONS.remove(userId);
        }
    }

    /**
     * Invalidate all sessions belonging to a user, optionally keeping one (e.g. the
     * session that initiated a self password change).
     *
     * @param userId          the user whose sessions to invalidate
     * @param exceptSessionId session id to keep alive, or {@code null} to invalidate all
     * @return number of sessions invalidated
     */
    public static int invalidateAllForUser(long userId, String exceptSessionId) {
        Map<String, HttpSession> m = SESSIONS.get(userId);
        if (m == null) return 0;
        int count = 0;
        for (HttpSession s : new ArrayList<>(m.values())) {
            if (exceptSessionId != null && exceptSessionId.equals(s.getId())) continue;
            m.remove(s.getId());
            try {
                s.invalidate();
                count++;
            } catch (IllegalStateException ignored) {
                // already invalidated
            }
        }
        if (m.isEmpty()) SESSIONS.remove(userId);
        return count;
    }
}
