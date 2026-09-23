package com.smipl.lcrecon.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * Adds security response headers to every response.
 * WEB_VUL_05 (Clickjacking): the app must not be embeddable in a frame by untrusted sites.
 * WEB_VUL_10 (SameSite): append SameSite=Lax to Set-Cookie (Tomcat 8.5.23 predates the
 *            sameSiteCookies CookieProcessor attribute, so it is enforced here at the app layer).
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        // Anti-clickjacking: block framing entirely (no internal iframe usage in this app)
        response.setHeader("X-Frame-Options", "DENY");
        // WEB_VUL_15 (CSP): restrict resource origins. 'unsafe-inline' is required for the app's
        // many inline <script>/handlers and inline styles; Stored/Reflected XSS is already fixed by
        // output encoding, so CSP here is defense-in-depth (blocks external script/object, framing,
        // base-uri and form-action hijacking).
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                + "script-src 'self' 'unsafe-inline'; "
                + "style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data: blob:; "
                + "font-src 'self'; "
                + "connect-src 'self'; "
                + "object-src 'none'; "
                + "base-uri 'self'; "
                + "form-action 'self'; "
                + "frame-ancestors 'none'");
        // WEB_VUL_16: prevent MIME-type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        // WEB_VUL_17: limit referrer leakage to other origins
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // WEB_VUL_18: disable powerful browser features the app does not use
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=(), interest-cohort=()");
        // WEB_VUL_19: cross-origin isolation (safe subset - COOP/CORP; COEP omitted to avoid
        // blocking legitimate same-origin subresource loads)
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        // WEB_VUL_07 / WEB_VUL_14 (HSTS): instruct browsers to use HTTPS only. Sent unconditionally so
        // it survives a TLS-terminating reverse proxy (browser<->proxy is HTTPS); browsers ignore it
        // over plain HTTP per RFC 6797.
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        chain.doFilter(req, res);

        // WEB_VUL_10: ensure SameSite=Lax on any Set-Cookie the container/app emitted
        if (!response.isCommitted()) {
            Collection<String> cookies = response.getHeaders("Set-Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                boolean first = true;
                for (String cookie : cookies) {
                    String value = cookie;
                    if (value != null && !value.toLowerCase().contains("samesite")) {
                        value = value + "; SameSite=Lax";
                    }
                    if (first) { response.setHeader("Set-Cookie", value); first = false; }
                    else { response.addHeader("Set-Cookie", value); }
                }
            }
        }
    }

    @Override
    public void destroy() {}
}
