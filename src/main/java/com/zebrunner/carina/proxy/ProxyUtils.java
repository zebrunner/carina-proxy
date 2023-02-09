package com.zebrunner.carina.proxy;

import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ProxyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ProxyUtils() {
        // hide
    }

    /**
     * Get Selenium proxy object<br>
     *
     * @return {@link Proxy} in {@link Optional} if according to the configuration it should have been created, {@link Optional#empty()} otherwise
     * @throws InvalidConfigurationException if the proxy configuration is incorrect
     */
    public static Optional<Proxy> getSeleniumProxy() {
        String proxyTypeAsString = getConfigurationValue("proxy_type");
        if (proxyTypeAsString.isEmpty()) {
            throw new InvalidConfigurationException("proxy_type should not be empty and have a correct value.");
        }

        // in the old approach, in any situation, before the formation of the object proxy,
        // after the possible start of the dynamic proxy, the system proxy was initialized.
        // We leave it for compatibility, even if in MANUAL and DYNAMIC mode the system proxy
        // will be overwritten again
        SystemProxy.setupProxy();

        if ("UNUSED".equalsIgnoreCase(proxyTypeAsString)) {
            return Optional.empty();
        }

        if ("LEGACY".equalsIgnoreCase(proxyTypeAsString)) {
            return getLegacyProxy();
        }

        if ("DYNAMIC".equalsIgnoreCase(proxyTypeAsString)) {
            return getDynamicSeleniumProxy();
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
            proxy = getManualSeleniumProxy();
            break;

        case PAC:
            String autoConfigURL = getConfigurationValue("proxy_autoconfig_url");
            if (autoConfigURL.isEmpty()) {
                throw new InvalidConfigurationException("ProxyType is PAC, but proxy_autoconfig_url is empty. Please, provide autoconfig url");
            }
            if (Boolean.parseBoolean(getConfigurationValue("proxy_pac_local"))) {
                Path path = Path.of(autoConfigURL);
                if (!Files.exists(path)) {
                    throw new InvalidConfigurationException("'proxy_pac_local' parameter value is true, "
                            + "but there is no file on the path specified in parameter 'proxy_autoconfig_url'. Path: " + path);
                }
                if (Files.isDirectory(path)) {
                    throw new InvalidConfigurationException("'proxy_pac_local' parameter value is true, "
                            + "but the path specified in the 'proxy_pac_local' parameter does not point to the file, "
                            + "but to the directory. Specify the path to the file. Path: " + path);
                }
                autoConfigURL = encodePAC(path);
            }
            proxy.setProxyAutoconfigUrl(autoConfigURL);
            break;
        case UNSPECIFIED:
            // do nothing - unspecified is set by default
            break;

        case AUTODETECT:
            proxy.setAutodetect(true);
            break;
        case SYSTEM:
            proxy.setProxyType(Proxy.ProxyType.SYSTEM);
            break;
        default:
            throw new InvalidConfigurationException("ProxyType was not detected.");
        }
        return Optional.of(proxy);
    }

    /**
     * Encode PAC file to encoded link with Base64
     *
     * @param pathToPac {@link Path} to the pac file
     * @return encoded link to pac file
     * @throws UncheckedIOException if error happens when try to read/encode content of the file
     */
    private static String encodePAC(Path pathToPac) {
        try {
            return String.format("data:application/x-javascript-config;base64,%s",
                    new String(Base64.getEncoder().encode(Files.readAllBytes(pathToPac))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Proxy getManualSeleniumProxy() {
        String proxyHost = Configuration.get(Configuration.Parameter.PROXY_HOST);
        String proxyPort = Configuration.get(Configuration.Parameter.PROXY_PORT);
        String noProxy = Configuration.get(Configuration.Parameter.NO_PROXY);
        // there are difference between system noproxy (parttern with '|' delimiter) and selenium noproxy (addresses with ',' delimiter)
        String systemNoProxy = noProxy.contains(",") ? Arrays.stream(noProxy.split(","))
                .map(String::trim)
                .collect(Collectors.joining("|")) : noProxy;

        List<String> protocols = Arrays.asList(Configuration.get(Configuration.Parameter.PROXY_PROTOCOLS).split("[\\s,]+"));
        boolean isSetToSystem = Configuration.getBoolean(Configuration.Parameter.PROXY_SET_TO_SYSTEM);

        if (proxyHost.isEmpty() || proxyPort.isEmpty() || protocols.isEmpty()) {
            throw new InvalidConfigurationException(
                    "Provided 'MANUAL' proxy type, but proxyHost, proxyPort or proxy protocols is empty. Please, provide valid configuration.");
        }

        org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
        String proxyAddress = String.format("%s:%s", proxyHost, proxyPort);

        if (protocols.contains("http")) {
            LOGGER.info("Http proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setHttpProxy(proxyAddress);
            if (isSetToSystem) {
                //todo investigate if we need to set system proxy here
                SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.HTTP, systemNoProxy);
            }
        }

        if (protocols.contains("https")) {
            LOGGER.info("Https proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setSslProxy(proxyAddress);
            if (isSetToSystem) {
                //todo investigate if we need to set system proxy here
                SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.HTTPS, systemNoProxy);
            }
        }

        if (protocols.contains("ftp")) {
            LOGGER.info("FTP proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setFtpProxy(proxyAddress);
            if (isSetToSystem) {
                //todo investigate if we need to set system proxy here
                SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.FTP, systemNoProxy);
            }
        }

        if (protocols.contains("socks")) {
            LOGGER.info("Socks proxy will be set: {}:{}", proxyHost, proxyPort);
            proxy.setSocksProxy(proxyAddress);
            if (isSetToSystem) {
                //todo investigate if we need to set system proxy here
                SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.SOCKS, systemNoProxy);
            }
        }

        if (!noProxy.isEmpty()) {
            proxy.setNoProxy(noProxy);
        }
        return proxy;
    }

    private static Optional<Proxy> getDynamicSeleniumProxy() {
        ProxyPool.startProxy();
        Optional<IProxy> proxy = ProxyPool.getProxy();

        if (proxy.isEmpty()) {
            return Optional.empty();
        }

        IProxyInfo proxyInfo = proxy.orElseThrow(() -> new RuntimeException("Proxy info should exists for starting proxy"))
                .getInfo();
        String noProxy = Configuration.get(Configuration.Parameter.NO_PROXY);
        boolean isSetToSystem = Configuration.getBoolean(Configuration.Parameter.PROXY_SET_TO_SYSTEM);
        // there are difference between system noproxy (parttern with '|' delimiter) and selenium noproxy (addresses with ',' delimiter)
        String systemNoProxy = noProxy.contains(",") ? Arrays.stream(noProxy.split(","))
                .map(String::trim)
                .collect(Collectors.joining("|")) : noProxy;

        if (isSetToSystem) {
            LOGGER.warn("Setting proxy to system parameters is not thread-safe with DYNAMIC proxy mode.");
        }

        List<String> protocols = Arrays.asList(Configuration.get(Configuration.Parameter.PROXY_PROTOCOLS).split("[\\s,]+"));
        List<Protocol> supportedProtocols = proxy.get()
                .getSupportedProtocols();
        String proxyHost = proxyInfo.getHost();
        String proxyPort = String.valueOf(proxyInfo.getPort());

        org.openqa.selenium.Proxy seleniumProxy = new org.openqa.selenium.Proxy();
        String proxyAddress = String.format("%s:%s", proxyHost, proxyPort);

        if (protocols.contains("http")) {
            if (supportedProtocols.contains(Protocol.HTTP)) {
                LOGGER.info("Http proxy will be set: {}:{}", proxyHost, proxyPort);
                seleniumProxy.setHttpProxy(proxyAddress);
                if (isSetToSystem) {
                    //todo investigate if we need to set system proxy here
                    SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.HTTP, systemNoProxy);
                }
            } else {
                LOGGER.warn("'proxy_protocols' configuration parameter contains 'http' protocol, but '{}' proxy implementation does not support it.",
                        proxy.get());
            }
        }

        if (protocols.contains("https")) {
            if (supportedProtocols.contains(Protocol.HTTPS)) {
                LOGGER.info("Https proxy will be set: {}:{}", proxyHost, proxyPort);
                seleniumProxy.setSslProxy(proxyAddress);
                if (isSetToSystem) {
                    //todo investigate if we need to set system proxy here
                    SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.HTTPS, systemNoProxy);
                }
            } else {
                LOGGER.warn("'proxy_protocols' configuration parameter contains 'https' protocol, but '{}' proxy implementation does not support it.",
                        proxy.get());
            }
        }

        if (protocols.contains("ftp")) {
            if (supportedProtocols.contains(Protocol.FTP)) {
                LOGGER.info("FTP proxy will be set: {}:{}", proxyHost, proxyPort);
                seleniumProxy.setFtpProxy(proxyAddress);
                if (isSetToSystem) {
                    //todo investigate if we need to set system proxy here
                    SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.FTP, systemNoProxy);
                }
            } else {
                LOGGER.warn("'proxy_protocols' configuration parameter contains 'ftp' protocol, but '{}' proxy implementation does not support it.",
                        proxy.get());
            }
        }

        if (protocols.contains("socks")) {
            if (supportedProtocols.contains(Protocol.SOCKS)) {
                LOGGER.info("Socks proxy will be set: {}:{}", proxyHost, proxyPort);
                seleniumProxy.setSocksProxy(proxyAddress);
                if (isSetToSystem) {
                    //todo investigate if we need to set system proxy here
                    SystemProxy.setupSystemProxy(proxyHost, proxyPort, Protocol.SOCKS, systemNoProxy);
                }
            } else {
                LOGGER.warn("'proxy_protocols' configuration parameter contains 'socks' protocol, but '{}' proxy implementation does not support it.",
                        proxy.get());
            }
        }

        if (!noProxy.isEmpty()) {
            seleniumProxy.setNoProxy(noProxy);
        }
        return Optional.of(seleniumProxy);
    }

    private static Optional<Proxy> getLegacyProxy() {
        com.zebrunner.carina.proxy.browserup.ProxyPool.setupBrowserUpProxy();
        SystemProxy.setupProxy();

        String proxyHost = Configuration.get(Configuration.Parameter.PROXY_HOST);
        String proxyPort = Configuration.get(Configuration.Parameter.PROXY_PORT);
        String noProxy = Configuration.get(Configuration.Parameter.NO_PROXY);

        if (Configuration.get(Configuration.Parameter.BROWSERUP_PROXY).equals("true")) {
            proxyPort = Integer.toString(com.zebrunner.carina.proxy.browserup.ProxyPool.getProxyPortFromThread());
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

    // todo remove when params will be added to the Configuration class
    private static String getConfigurationValue(String param) {
        String value = R.CONFIG.get(param);
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }

}
