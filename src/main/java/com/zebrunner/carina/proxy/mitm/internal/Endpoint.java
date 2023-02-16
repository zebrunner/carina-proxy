package com.zebrunner.carina.proxy.mitm.internal;

import com.zebrunner.carina.proxy.mitm.entity.Message;
import com.zebrunner.carina.proxy.mitm.entity.MessageSettings;
import com.zebrunner.carina.proxy.mitm.entity.PreRequest;
import com.zebrunner.carina.proxy.mitm.entity.PreResponse;
import com.zebrunner.carina.proxy.mitm.filter.HttpRequestFilter;
import com.zebrunner.carina.proxy.mitm.filter.HttpResponseFilter;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

@ClientEndpoint
public class Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private HttpRequestFilter httpRequestFilter = null;
    private HttpResponseFilter httpResponseFilter = null;

    /**
     * Set filter for changing request
     *
     * @param filter see {@link HttpRequestFilter}
     */
    public void setHttpRequestFilter(HttpRequestFilter filter) {
        this.httpRequestFilter = filter;
    }

    /**
     * Set filter for changing response
     *
     * @param filter see {@link HttpResponseFilter}
     */
    public void setHttpResponseFilter(HttpResponseFilter filter) {
        this.httpResponseFilter = filter;
    }

    @OnOpen
    public void onOpen(Session session) {
        LOGGER.info("[CLIENT]: Connection established..... \n[CLIENT]: Session ID: {}", session.getId());
        try {
            session.getBasicRemote().sendText("Server is ready.....");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        try {
            Optional<PreRequest> preRequest = parsePreRequest(message);
            if (preRequest.isPresent() && "pre_request".equalsIgnoreCase(preRequest.get().getStage())) {
                return onPreRequest(preRequest.get());
            }

            Optional<PreResponse> preResponse = parsePreResponse(message);
            if (preResponse.isPresent() && "pre_response".equalsIgnoreCase(preResponse.get().getStage())) {
                return onPreResponse(preResponse.get());
            }

            Optional<Message> optionalRequestOrResponse = parseMessage(message);
            if (optionalRequestOrResponse.isPresent()) {
                Message requestOrResponse = optionalRequestOrResponse.get();
                if ("request".equalsIgnoreCase(requestOrResponse.getStage())) {
                    return onRequest(requestOrResponse);
                } else if ("response".equalsIgnoreCase(requestOrResponse.getStage())) {
                    return onResponse(requestOrResponse);
                } else {
                    // should never happen
                    throw new RuntimeException("Getted request/response-like message, but stage name is not equals request or response.");
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ERROR]: {}", e.getMessage(), e);
            return message;
        }
        // should never happens
        LOGGER.warn("[NOT-KNOWN MESSAGE]: {}", message);
        return message;
    }

    private String onPreRequest(PreRequest preRequest) throws EncodeException {
        LOGGER.info("[PRE-REQUEST]: {}", preRequest);
        MessageSettings messageSettings = new MessageSettings();
        messageSettings.setId(preRequest.getId());
        messageSettings.setSendRequest(true);
        messageSettings.setSendResponse(true);
        return new MessageSettingsEncoder().encode(messageSettings);
    }

    private String onRequest(Message request) throws EncodeException {
        LOGGER.info("[REQUEST]: {}", request);
        if (httpRequestFilter != null) {
            // mutate request
            httpRequestFilter.filter(request);
        }
        return new MessageEncoder().encode(request);
    }

    private String onPreResponse(PreResponse preRequest) throws EncodeException {
        LOGGER.info("[PRE-RESPONSE]: {}", preRequest);
        MessageSettings messageSettings = new MessageSettings();
        messageSettings.setId(preRequest.getId());
        messageSettings.setSendRequest(true);
        messageSettings.setSendResponse(true);
        return new MessageSettingsEncoder().encode(messageSettings);
    }

    private String onResponse(Message response) throws EncodeException {
        LOGGER.info("[RESPONSE]: {}", response);
        if (httpResponseFilter != null) {
            // mutate response
            httpResponseFilter.filter(response);
        }
        return new MessageEncoder().encode(response);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOGGER.warn("[CLIENT]: Session {}  close, because {}", session.getId(), closeReason);
    }

    @OnError
    public void onError(Session session, Throwable err) {
        LOGGER.error("[CLIENT]: Error!!!!!, Session ID: {}, {}", session.getId(), err.getMessage());
    }

    private static Optional<PreRequest> parsePreRequest(String message) throws DecodeException {
        PreRequestDecoder decoder = new PreRequestDecoder();
        PreRequest preRequest = null;
        if (decoder.willDecode(message)) {
            preRequest = decoder.decode(message);
        }
        return Optional.ofNullable(preRequest);
    }

    private static Optional<PreResponse> parsePreResponse(String message) throws DecodeException {
        PreResponseDecoder decoder = new PreResponseDecoder();
        PreResponse preRequest = null;
        if (decoder.willDecode(message)) {
            preRequest = decoder.decode(message);
        }
        return Optional.ofNullable(preRequest);
    }

    private static Optional<Message> parseMessage(String message) throws DecodeException {
        MessageDecoder decoder = new MessageDecoder();
        Message request = null;
        if (decoder.willDecode(message)) {
            request = decoder.decode(message);
        }
        return Optional.ofNullable(request);
    }
}
