package com.smipl.lcrecon.listener;

import com.smipl.lcrecon.model.User;
import com.smipl.lcrecon.util.SessionRegistry;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Keeps {@link SessionRegistry} in sync by removing sessions when they are destroyed
 * (logout, timeout, or explicit invalidation).
 */
@WebListener
public class SessionCleanupListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // no-op; sessions are registered at login once the user is known
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Object user = session.getAttribute("user");
        if (user instanceof User) {
            SessionRegistry.unregister(((User) user).getId(), session.getId());
        }
    }
}
