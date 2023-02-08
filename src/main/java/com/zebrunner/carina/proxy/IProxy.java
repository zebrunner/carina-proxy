package com.zebrunner.carina.proxy;

import java.util.List;

/**
 * Describes methods used by {@link ProxyPool} to manage proxy state.<br>
 * Is an abstraction over any proxy implementation.
 */
public interface IProxy {

    /**
     * Starts the proxy on the specified port.
     *
     * @param port port to listen on
     * @throws java.lang.IllegalStateException if the proxy has already been started
     */
    IProxyInfo start(int port);

    /**
     * Stop proxy
     *
     * @throws java.lang.IllegalStateException if the proxy has not been started.
     */
    void stop();

    /**
     * Get information about started proxy
     *
     * @return see {@link IProxyInfo}
     * @throws java.lang.IllegalStateException if the proxy has not been started.
     */
    IProxyInfo getInfo();

    /**
     * Check if the proxy is started
     *
     * @return true if is proxy started, false otherwise
     */
    boolean isStarted();

    /**
     * Get protocols, supported by this proxy implementation
     *
     * @return {@link  List} of {@link @Pro}
     */
    default List<Protocol> getSupportedProtocols() {
        return List.of(Protocol.HTTP, Protocol.HTTPS);
    }
}
