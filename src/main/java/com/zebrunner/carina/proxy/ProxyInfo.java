package com.zebrunner.carina.proxy;

/**
 * Default {@link IProxyInfo} implementation
 */
public class ProxyInfo implements IProxyInfo {

    private final String host;
    private final Integer port;

    public ProxyInfo(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public Integer getPort() {
        return this.port;
    }
}
