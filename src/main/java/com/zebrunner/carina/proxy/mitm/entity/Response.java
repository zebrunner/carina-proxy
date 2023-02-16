package com.zebrunner.carina.proxy.mitm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;
import java.util.List;
import java.util.Map;

public class Response {

    private String httpVersion;

    private Integer statusCode;

    private String reason;

    private Map<String, List<String>> header;

    private String body;

    private List<String> trailers;

    private Number timeStampStart;

    private Number timeStampEnd;

    public String getHttpVersion() {
        return httpVersion;
    }

    @JsonProperty("http_version")
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

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

    public Map<String, List<String>> getHeader() {
        return header;
    }

    @JsonProperty("headers")
    public void setHeader(Map<String, List<String>> header) {
        this.header = header;
    }

    public String getBody() {
        return body;
    }

    public String getDecodedBody() {
        return new String(Base64.getDecoder().decode(this.body));
    }

    public void setDecodedBody(String body) {
        this.body = Base64.getEncoder().encodeToString(body.getBytes());
    }

    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getTrailers() {
        return trailers;
    }

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @JsonProperty("trailers")
    public void setTrailers(List<String> trailers) {
        this.trailers = trailers;
    }

    public Number getTimeStampStart() {
        return timeStampStart;
    }

    @JsonProperty("timestamp_start")
    public void setTimeStampStart(Number timeStampStart) {
        this.timeStampStart = timeStampStart;
    }

    public Number getTimeStampEnd() {
        return timeStampEnd;
    }

    @JsonProperty("timestamp_end")
    public void setTimeStampEnd(Number timeStampEnd) {
        this.timeStampEnd = timeStampEnd;
    }

    @Override public String toString() {
        return "Response{" +
                "httpVersion='" + httpVersion + '\'' +
                ", statusCode=" + statusCode +
                ", reason='" + reason + '\'' +
                ", header=" + header +
                ", body='" + body + '\'' +
                ", trailers=" + trailers +
                ", timeStampStart=" + timeStampStart +
                ", timeStampEnd=" + timeStampEnd +
                '}';
    }
}
