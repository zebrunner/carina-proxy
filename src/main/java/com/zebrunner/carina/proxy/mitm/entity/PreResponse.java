package com.zebrunner.carina.proxy.mitm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreResponse {

    private String stage;

    private String flowId;

    private PreRequest.RequestSummary requestSummary;

    private PreRequest.ResponseSummary responseSummary;

    private String id;

    public String getFlowId() {
        return flowId;
    }

    @JsonProperty("flow_id")
    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getStage() {
        return stage;
    }

    @JsonProperty("stage")
    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public PreRequest.RequestSummary getRequestSummary() {
        return requestSummary;
    }

    @JsonProperty("request_summary")
    public void setRequestSummary(PreRequest.RequestSummary requestSummary) {
        this.requestSummary = requestSummary;
    }

    public PreRequest.ResponseSummary getResponseSummary() {
        return responseSummary;
    }

    @JsonProperty("response_summary")
    public void setResponseSummary(PreRequest.ResponseSummary responseSummary) {
        this.responseSummary = responseSummary;
    }

    static class RequestSummary {

        private String method;

        private String url;

        public String getMethod() {
            return method;
        }

        @JsonProperty("method")
        public void setMethod(String method) {
            this.method = method;
        }

        public String getUrl() {
            return url;
        }

        @JsonProperty("url")
        public void setUrl(String url) {
            this.url = url;
        }

        @Override public String toString() {
            return "RequestSummary{" +
                    "method='" + method + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    static class ResponseSummary {

        private Integer statusCode;

        private String reason;

        public Integer getStatusCode() {
            return statusCode;
        }

        @JsonProperty("status_code")
        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        public String getReason() {
            return reason;
        }

        @JsonProperty("reason")
        public void setReason(String reason) {
            this.reason = reason;
        }

        @Override public String toString() {
            return "ResponseSummary{" +
                    "statusCode=" + statusCode +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    @Override public String toString() {
        return "PreResponse{" +
                "stage='" + stage + '\'' +
                ", flowId='" + flowId + '\'' +
                ", requestSummary=" + requestSummary +
                ", responseSummary=" + responseSummary +
                ", id='" + id + '\'' +
                '}';
    }
}
