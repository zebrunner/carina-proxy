package com.zebrunner.carina.proxy.mitm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageSettings {

    private String id;

    private Boolean isSendRequest;

    private Boolean isSendResponse;

    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public boolean isSendRequest() {
        return isSendRequest;
    }

    @JsonProperty(value = "send_request")
    public void setSendRequest(boolean sendRequest) {
        isSendRequest = sendRequest;
    }

    public boolean isSendResponse() {
        return isSendResponse;
    }

    @JsonProperty(value = "send_response")
    public void setSendResponse(boolean sendResponse) {
        isSendResponse = sendResponse;
    }
}
