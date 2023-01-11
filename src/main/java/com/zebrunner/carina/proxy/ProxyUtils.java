package com.zebrunner.carina.proxy;

import com.zebrunner.carina.proxy.browserup.ProxyPool;
import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import org.openqa.selenium.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum ProxyUtils {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Get Selenium proxy object
     *
     * @return {@link Proxy} in {@link Optional} if according to the configuration it should have been created, {@link Optional#empty()} otherwise
     * @throws InvalidConfigurationException if the proxy configuration in the configuration file is incorrect
     */
    public Optional<Proxy> getSeleniumProxy() {
        String proxyTypeAsString = R.CONFIG.get("proxy_type");
        if (proxyTypeAsString.isEmpty()) {
            throw new InvalidConfigurationException("proxy_type should not be empty.");
        }

        if ("UNUSED".equalsIgnoreCase(proxyTypeAsString)) {
            return Optional.empty();
        }

        Proxy.ProxyType proxyType;

        try {
            proxyType = Proxy.ProxyType.valueOf(proxyTypeAsString);
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException(String.format("There are no '%s' proxy type. Please, provide valid value", proxyTypeAsString));
        }

        Proxy proxy = new Proxy();
        switch (proxyType) {
        case DIRECT:
            proxy.setProxyType(Proxy.ProxyType.DIRECT);
            break;

        case MANUAL:
            Optional<Proxy> optionalProxy = getLegacyProxy();
            if (optionalProxy.isPresent()) {
                proxy = optionalProxy.get();
            } else {
                throw new InvalidConfigurationException(
                        "Provided 'MANUAL' proxy type, but cannot instantiate proxy. Please, check your configuration");
            }
            break;

        case PAC:
            String autoConfigURL = R.CONFIG.get("proxy_autoconfig_url");
            if (autoConfigURL.isEmpty()) {
                throw new InvalidConfigurationException("ProxyType is PAC, but proxy_autoconfig_url is empty. Please, provide autoconfig url");
            }
            proxy.setProxyAutoconfigUrl(autoConfigURL);
            break;
        case UNSPECIFIED:
            // todo refactor
            // use old method if proxy type was not specified
            Optional<Proxy> optionalProxy2 = getLegacyProxy();
            if (optionalProxy2.isPresent()) {
                proxy = optionalProxy2.get();
            }
            break;

        case AUTODETECT:
            proxy.setAutodetect(true);
            break;
        case SYSTEM:
            proxy.setProxyType(Proxy.ProxyType.SYSTEM);
            break;
        default:
            LOGGER.error("ProxyType was not detected.");
        }
        return Optional.of(proxy);
    }

    private Optional<Proxy> getLegacyProxy() {
        ProxyPool.setupBrowserUpProxy();
        SystemProxy.setupProxy();

        String proxyHost = Configuration.get(Configuration.Parameter.PROXY_HOST);
        String proxyPort = Configuration.get(Configuration.Parameter.PROXY_PORT);
        String noProxy = Configuration.get(Configuration.Parameter.NO_PROXY);

        if (Configuration.get(Configuration.Parameter.BROWSERUP_PROXY).equals("true")) {
            proxyPort = Integer.toString(ProxyPool.getProxyPortFromThread());
        }
        List<String> protocols = Arrays.asList(Configuration.get(Configuration.Parameter.PROXY_PROTOCOLS).split("[\\s,]+"));

        if (proxyHost.isEmpty() || proxyPort.isEmpty()) {
            return Optional.empty();
        }

        org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
        String proxyAddress = String.format("%s:%s", proxyHost, proxyPort);

        if (protocols.contains("http")) {
            LOGGER.info("Http proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setHttpProxy(proxyAddress);
        }

        if (protocols.contains("https")) {
            LOGGER.info("Https proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setSslProxy(proxyAddress);
        }

        if (protocols.contains("ftp")) {
            LOGGER.info("FTP proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setFtpProxy(proxyAddress);
        }

        if (protocols.contains("socks")) {
            LOGGER.info("Socks proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setSocksProxy(proxyAddress);
        }

        if (!noProxy.isEmpty()) {
            proxy.setNoProxy(noProxy);
        }
        return Optional.of(proxy);
    }
}
