package com.zebrunner.carina.proxy;

import java.util.Optional;

/**
 * Abstraction over proxy rule implementations
 */
public interface IProxyRule {

    /**
     * Get proxy instance
     *
     * @return {@link Optional} of {@link IProxy} if proxy should be started by Carina Framework before driver starting in DYNAMIC mode,
     * {@link Optional#empty()} otherwise
     */
    public Optional<IProxy> getProxyInstance();
}
