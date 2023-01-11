package com.zebrunner.carina.proxy;

import com.zebrunner.carina.proxy.browserup.ProxyPool;
import com.zebrunner.carina.utils.Configuration;
import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
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

public final class ProxyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ProxyUtils() {
        // hide
    }

    /**
     * Get Selenium proxy object
     *
     * @return {@link Proxy} in {@link Optional} if according to the configuration it should have been created, {@link Optional#empty()} otherwise
     * @throws InvalidConfigurationException if the proxy configuration in the configuration file is incorrect
     */
    public static Optional<Proxy> getSeleniumProxy() {
        String proxyTypeAsString = R.CONFIG.get("proxy_type");
        if (proxyTypeAsString.isEmpty()) {
            throw new InvalidConfigurationException("proxy_type should not be empty.");
        }

        if ("UNUSED".equalsIgnoreCase(proxyTypeAsString)) {
            return Optional.empty();
        }

        if ("LEGACY".equalsIgnoreCase(proxyTypeAsString)) {
            return getLegacyProxy();
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
            if (Boolean.parseBoolean(R.CONFIG.get("proxy_pac_local"))) {
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
            throw new RuntimeException("ProxyType was not detected.");
        }
        return Optional.of(proxy);
    }

    /**
     * Encode PAC file to encoded link with Base64
     *
     * @param pathToPac {@link Path} to *.pac file
     * @return encoded link to pac file
     * @throws IOException
     */
    private static String encodePAC(Path pathToPac) {
        try {
            return String.format("data:application/x-javascript-config;base64,%s",
                    new String(Base64.getEncoder().encode(Files.readAllBytes(pathToPac))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Proxy> getLegacyProxy() {
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
