package com.zebrunner.carina.proxy.mitm.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zebrunner.carina.proxy.mitm.entity.MessageSettings;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class MessageSettingsDecoder implements Decoder.Text<MessageSettings> {

    @Override
    public MessageSettings decode(String s) throws DecodeException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(s);
            MessageSettings preRequest = mapper.treeToValue(rootNode, MessageSettings.class);
            return preRequest;
        } catch (Exception e) {
            throw new DecodeException(s, "Error when decode message settings.", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(s);
            // should generate exception if s is not suitable
            mapper.treeToValue(rootNode, MessageSettings.class);
            return true;
        } catch (Exception e) {
            return false;
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
