package com.benbanking.api.auth;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestAuth {

    private RequestAuth() {
    }

    public static AuthSession requireSession(HttpServletRequest request) {
        Object value = request.getAttribute(SessionService.REQUEST_ATTRIBUTE);
        if (value instanceof AuthSession session) {
            return session;
        }
        throw new IllegalStateException("Authenticated session not found on request.");
    }
}
