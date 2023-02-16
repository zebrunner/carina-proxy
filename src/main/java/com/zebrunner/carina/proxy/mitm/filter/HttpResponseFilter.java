package com.zebrunner.carina.proxy.mitm.filter;

import com.zebrunner.carina.proxy.mitm.entity.Message;

public interface HttpResponseFilter {

    /**
     * Implement this method to modify an HTTP response.<br>
     * Modifications to the response object will be reflected in the client response.
     *
     * @param response see {@link Message}
     */
    void filter(Message response);
}
