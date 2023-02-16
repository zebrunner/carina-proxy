package com.zebrunner.carina.proxy.mitm.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zebrunner.carina.proxy.mitm.entity.PreResponse;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class PreResponseEncoder implements Encoder.Text<PreResponse> {

    @Override
    public String encode(PreResponse s) throws EncodeException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.valueToTree(s);
            return mapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new EncodeException(s, "Error when encode pre-response.", e);
        }
    }

    @Override
    public void init(EndpointConfig config) {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }
}
