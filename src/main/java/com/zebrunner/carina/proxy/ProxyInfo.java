package com.zebrunner.carina.proxy;

/**
 * Default {@link IProxyInfo} implementation
 */
public class ProxyInfo implements IProxyInfo {

    private final Integer port;

    public ProxyInfo(Integer port) {
        this.port = port;
    }

    @Override
    public Integer getPort() {
        return this.port;
    }

}
