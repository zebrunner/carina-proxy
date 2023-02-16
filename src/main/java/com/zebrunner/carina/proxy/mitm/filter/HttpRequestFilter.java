package com.zebrunner.carina.proxy.mitm.filter;

import com.zebrunner.carina.proxy.mitm.entity.Message;

@FunctionalInterface
public interface HttpRequestFilter {
    /**
     * Implement this method to modify an HTTP request.<br>
     * Modifications to the request object will be reflected in the client request.
     *
     * @param request see {@link Message}
     */
    void filter(Message request);
}
