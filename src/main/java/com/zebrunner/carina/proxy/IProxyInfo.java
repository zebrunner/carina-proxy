package com.zebrunner.carina.proxy;

/**
 * Describes information that can be obtained from a running proxy
 */
public interface IProxyInfo {

    /**
     * Get proxy host
     *
     * @return proxy host as string, for example {@code 0.0.0.0}
     */
    String getHost();

    /**
     * Get proxy port
     *
     * @return port, for example {@code 80}
     */
    Integer getPort();
}
