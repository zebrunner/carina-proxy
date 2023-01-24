package com.zebrunner.carina.proxy.browserup;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.zebrunner.carina.proxy.IProxy;
import com.zebrunner.carina.proxy.IProxyInfo;
import com.zebrunner.carina.proxy.ProxyInfo;
import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.common.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Default {@link IProxy} implementation, that use {@link BrowserUpProxy}
 */
public class CarinaBrowserUpProxy implements IProxy {
    // todo investigate using Process / Runnable / Callable to start BrowserUpProxy
    // when we kill parent process, all child processes also will be killed?
    // todo should be replaced by mitmproxy

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected final BrowserUpProxy proxy;
    protected IProxyInfo proxyInfo = null;

    public CarinaBrowserUpProxy() {
        proxy = new BrowserUpProxyServer();
        proxy.setTrustAllServers(true);
        // disable MITM in case we do not need it
        proxy.setMitmDisabled(Configuration.getBoolean(Configuration.Parameter.BROWSERUP_MITM));
    }

    @Override
    public IProxyInfo start(int port) {
        if (proxy.isStarted()) {
            throw new IllegalStateException("Proxy already started.");
        }
        LOGGER.info("Starting BrowserUp proxy...");
        proxy.start(port);

        CommonUtils.pause(2); // todo set pause or not?
        proxyInfo = new ProxyInfo(proxy.getPort());
        return proxyInfo;
    }

    @Override
    public void stop() {
        if (!proxy.isStarted()) {
            throw new IllegalStateException("Proxy was not started.");
        }
        // isStarted returns true even if proxy was already stopped
        try {
            LOGGER.debug("stopProxy starting...");
            proxy.stop();
        } catch (IllegalStateException e) {
            LOGGER.info("Seems like proxy was already stopped.");
            LOGGER.info(e.getMessage());
        } finally {
            proxyInfo = null;
            LOGGER.debug("stopProxy finished...");
        }
    }

    @Override
    public IProxyInfo getInfo() {
        if (!isStarted()) {
            throw new IllegalStateException("Proxy was not started.");
        }
        return proxyInfo;
    }

    @Override
    public boolean isStarted() {
        return proxy.isStarted();
    }

    /**
     * Get object of BrowserUp proxy
     *
     * @return {@link BrowserUpProxy}
     */
    public BrowserUpProxy getProxy() {
        return proxy;
    }

}
