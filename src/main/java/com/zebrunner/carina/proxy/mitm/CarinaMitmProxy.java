package com.zebrunner.carina.proxy.mitm;

import com.zebrunner.carina.proxy.IProxy;
import com.zebrunner.carina.proxy.IProxyInfo;
import com.zebrunner.carina.proxy.ProxyInfo;
import com.zebrunner.carina.proxy.mitm.filter.HttpRequestFilter;
import com.zebrunner.carina.proxy.mitm.filter.HttpResponseFilter;
import com.zebrunner.carina.proxy.mitm.internal.Endpoint;
import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.client.ClientManager;

import java.net.URI;
import java.net.URISyntaxException;

public class CarinaMitmProxy implements IProxy {

    private ClientManager client;
    private Endpoint endpoint;
    private IProxyInfo proxyInfo;
    private boolean isStarted = false;
    private final String mitmWebSocketURL;

    public CarinaMitmProxy() {
        this.client = ClientManager.createClient();
        String mitmWebsocketUrl = R.CONFIG.get("proxy_mitm_ws_url");
        if (mitmWebsocketUrl.isBlank()) {
            throw new InvalidConfigurationException("'proxy_mitm_ws_url' configuration parameter should not be empty.");
        }
        this.mitmWebSocketURL = mitmWebsocketUrl;
        this.endpoint = new Endpoint();
    }

    @Override
    public IProxyInfo start(int port) {
        try {
            this.client.asyncConnectToServer(this.endpoint, new URI(mitmWebSocketURL));
        } catch (DeploymentException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String proxyHost = Configuration.get(Configuration.Parameter.PROXY_HOST);
        if (proxyHost.isBlank()) {
            throw new InvalidConfigurationException("'proxy_host' configuration parameter should contains host of proxy.");
        }

        int proxyPort;
        try {
            proxyPort = Configuration.getInt(Configuration.Parameter.PROXY_PORT);
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(
                    "'proxy_port' configuration parameter contains invalid value. Original exception: " + e.getMessage());
        }

        this.proxyInfo = new ProxyInfo(proxyHost, proxyPort);
        this.isStarted = true;
        return this.proxyInfo;
    }

    @Override
    public void stop() {
        this.isStarted = false;
        this.client = null;
        this.endpoint = null;
        this.proxyInfo = null;
    }

    @Override
    public IProxyInfo getInfo() {
        if (!isStarted()) {
            throw new IllegalStateException("Proxy is not started.");
        }
        return this.proxyInfo;
    }

    @Override
    public boolean isStarted() {
        return this.isStarted;
    }

    /**
     * Set filter for changing request.<br>
     * The addition can take place both before the proxy is started and while the proxy is running.
     *
     * @param filter see {@link HttpRequestFilter}
     */
    public void setHttpRequestFilter(HttpRequestFilter filter) {
        this.endpoint.setHttpRequestFilter(filter);
    }

    /**
     * Set filter for changing response.<br>
     * The addition can take place both before the proxy is started and while the proxy is running.
     *
     * @param filter see {@link HttpResponseFilter}
     */
    public void setHttpResponseFilter(HttpResponseFilter filter) {
        this.endpoint.setHttpResponseFilter(filter);
    }

}
