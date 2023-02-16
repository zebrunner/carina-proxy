package com.zebrunner.carina.proxy.mitm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;
import java.util.List;
import java.util.Map;

public class Request {

    private String httpVersion;

    private String method;

    private String url;

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

    @JsonProperty("method")
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    public String getDecodedBody() {
        return new String(Base64.getDecoder().decode(this.body));
    }

    public void setDecodedBody(String body) {
        this.body = Base64.getEncoder().encodeToString(body.getBytes());
    }

    public List<String> getTrailers() {
        return trailers;
    }

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
        return "Request{" +
                "httpVersion='" + httpVersion + '\'' +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", header=" + header +
                ", body='" + body + '\'' +
                ", trailers=" + trailers +
                ", timeStampStart=" + timeStampStart +
                ", timeStampEnd=" + timeStampEnd +
                '}';
    }
}
