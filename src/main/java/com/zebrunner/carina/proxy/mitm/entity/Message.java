package com.zebrunner.carina.proxy.mitm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {

    private String stage;

    private String flowId;

    private String id;

    private Request request;

    private Response response;

    public String getStage() {
        return stage;
    }

    @JsonProperty("stage")
    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getFlowId() {
        return flowId;
    }

    @JsonProperty("flow_id")
    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Request getRequest() {
        return request;
    }

    @JsonProperty("request")
    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    @JsonProperty("response")
    public void setResponse(Response response) {
        this.response = response;
    }

    @Override public String toString() {
        return "Message{" +
                "stage='" + stage + '\'' +
                ", flowId='" + flowId + '\'' +
                ", id='" + id + '\'' +
                ", request=" + request +
                ", response=" + response +
                '}';
    }
}
