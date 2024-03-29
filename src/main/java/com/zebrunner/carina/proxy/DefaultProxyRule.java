package com.zebrunner.carina.proxy;

import com.zebrunner.carina.proxy.browserup.CarinaBrowserUpProxy;
import com.zebrunner.carina.utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

/**
 * Default proxy rule implementation of {@link IProxyRule}
 */
public class DefaultProxyRule implements IProxyRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Optional<IProxy> getProxyInstance() {
        IProxy proxy = null;
        if (Configuration.getBoolean(Configuration.Parameter.BROWSERUP_PROXY)) {
            proxy = new CarinaBrowserUpProxy();
        } else {
            LOGGER.debug("Proxy is disabled.");
        }
        return Optional.ofNullable(proxy);
    }

}
