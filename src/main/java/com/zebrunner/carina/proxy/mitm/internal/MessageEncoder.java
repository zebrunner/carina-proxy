package com.zebrunner.carina.proxy.mitm.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zebrunner.carina.proxy.mitm.entity.Message;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<Message> {

    @Override
    public String encode(Message s) throws EncodeException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.valueToTree(s);
            return mapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new EncodeException(s, "Error when encode message.", e);
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
