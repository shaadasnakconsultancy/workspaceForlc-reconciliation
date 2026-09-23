package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.util.CryptoUtil;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Exposes the RSA public key so the login page can encrypt the password before submission
 * (VAPT WEB_VUL_07). Public key material only - safe to serve unauthenticated.
 */
@WebServlet("/login-key")
public class LoginKeyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonUtil.writeJsonResponse(response,
                Collections.singletonMap("publicKey", CryptoUtil.getPublicKeySpkiBase64()));
    }
}
