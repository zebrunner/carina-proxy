package com.zebrunner.carina.proxy;

import com.zebrunner.carina.utils.R;
import com.zebrunner.carina.utils.commons.SpecialKeywords;
import com.zebrunner.carina.utils.exception.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation-independent proxy pool.
 * <p>
 * The creation of a proxy <b>depends on the rule</b>.<br>
 * The rule can be created by the user by implementing {@link IProxyRule} interface and setted via methods:<br>
 * {@link #setRule(IProxyRule)} - set global rule<br>
 * {@link #setRule(IProxyRule, boolean)} - allows you to specify whether the rule will be used only for the current thread<br>
 * <p>
 * Also, if you need to create your own proxy (for example, if you do not want to use the default proxy
 * (in this case, {@link com.zebrunner.carina.proxy.browserup.CarinaBrowserUpProxy}), then you can implement the
 * {@link IProxy} interface and reuse your proxy implementation in your custom rule or register it for current thread using
 * {@link #register(IProxy)} method.
 *
 * <p>
 * Configuration:
 * <b>proxy_port</b> - proxy port, that will be used by default for proxy starting. Default value: 0
 * <b>proxy_ports</b> - proxy port(s), that will be used to start proxy. They will be used only in case when proxy_port is NULL
 *
 * Default proxy (that use BrowserUp proxy) depends on configuration:
 * <b>browserup_proxy</b> - true if proxy should be started, false otherwise
 * <b>browserup_disabled_mitm</b> - when true, MITM capture will be disabled, false otherwise
 * <br>
 * <b>Important</b>: the proxy will be launched by Carina Framework before driver starts when generation capabilities only if the <b>DYNAMIC</b> mode is enabled in the configuration (<b>proxy_type=DYNAMIC</b>)
 */
public class ProxyPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Map<Long, IProxy> PROXY_POOL = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> PROXY_PORTS_FROM_RANGE = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> PROXY_PORTS_BY_THREAD = new ConcurrentHashMap<>();
    private static IProxyRule globalRule = new DefaultProxyRule(); // global proxy rule
    //todo investigate how to clean (maybe use Map to store all thread-depended rules and clean them)
    private static final ThreadLocal<IProxyRule> THREAD_RULE = new ThreadLocal<>(); // thread-only proxy rule

    static {
        initProxyPortsRange();
    }

    private ProxyPool() {
        // hide
    }

    /**
     * Set global proxy rule
     *
     * @param rule see {@link IProxyRule}
     */
    public static void setRule(IProxyRule rule) {
        setRule(rule, false);
    }

    /**
     * Set proxy rule
     *
     * @param rule       see {@link IProxyRule}
     * @param threadOnly should the rule only work for the current thread
     */
    public static void setRule(IProxyRule rule, boolean threadOnly) {
        if (threadOnly == true) {
            THREAD_RULE.set(rule);
        } else {
            ProxyPool.globalRule = rule;
        }
    }

    /**
     * Get current proxy rule
     *
     * @return {@link IProxyRule}
     */
    public static IProxyRule getRule() {
        IProxyRule proxyRule = THREAD_RULE.get();
        if (proxyRule == null) {
            proxyRule = globalRule;
        }
        return proxyRule;
    }

    /**
     * Start proxy (depends on {@link IProxyRule} implementation(s))<br>
     *
     * @return {@link Optional} of {@link ProxyInfo} if proxy started (should it be started or not depends on rule),
     * {@link Optional#empty()} otherwise
     */
    public static Optional<IProxyInfo> startProxy() {
        Optional<IProxy> proxy = getRule().getProxyInstance();
        IProxyInfo proxyInfo = null;
        if (proxy.isPresent()) {
            proxyInfo = startProxy(proxy.get());
        }
        return Optional.ofNullable(proxyInfo);
    }

    /**
     * Start proxy explicitly, ignoring {@link IProxyRule}s<br>
     * If proxy already exists in current thread, it will not be override
     *
     * @param proxy see {@link IProxy}
     * @return {@link ProxyInfo}
     */
    public static IProxyInfo startProxy(IProxy proxy) {
        long threadId = Thread.currentThread().getId();
        Integer availablePort;
        if (PROXY_PORTS_BY_THREAD.containsKey(threadId)) {
            LOGGER.warn("Existing proxy ports is detected in current thread and will be used to start current proxy.");
            availablePort = PROXY_PORTS_BY_THREAD.get(threadId);
        } else {
            availablePort = getProxyPortFromConfig();
        }

        IProxyInfo proxyInfo;
        if (PROXY_POOL.containsKey(threadId)) {
            LOGGER.warn("Existing proxy is detected in current thread.");
            if (!PROXY_POOL.get(threadId).isStarted()) {
                PROXY_POOL.get(threadId).start(availablePort);
            }
            proxyInfo = PROXY_POOL.get(threadId)
                    .getInfo();
        } else {
            proxyInfo = proxy.start(availablePort);
            PROXY_POOL.put(threadId, proxy);
        }

        PROXY_PORTS_BY_THREAD.put(threadId, proxyInfo.getPort());
        return proxyInfo;
    }

    /**
     * Register proxy in current thread. Proxy will not started by this method.
     * Also, it should not be started by user before registering.
     * <p>
     * If proxy already exists in current thread, it will be stopped and removed from current thread.
     * Port that used by this proxy will be reused.
     *
     * @param proxy {@link IProxy}
     */
    public static void register(IProxy proxy) {
        long threadId = Thread.currentThread().getId();
        if (PROXY_POOL.containsKey(threadId)) {
            LOGGER.warn("Existing proxy is detected and will be stopped and overwritten");
            PROXY_POOL.get(threadId)
                    .stop();
            PROXY_POOL.remove(threadId);
        }
        LOGGER.info("Register custom proxy in thread: {}", threadId);
        PROXY_POOL.put(threadId, proxy);
    }

    /**
     * Check is proxy exists (registered) in current thread
     *
     * @return true if proxy registered (exists) in pool in current thread, false otherwise
     */
    public static boolean isProxyRegistered() {
        long threadId = Thread.currentThread().getId();
        return PROXY_POOL.containsKey(threadId);
    }

    /**
     * Get proxy of current thread
     *
     * @return {@link Optional} of {@link IProxy} if there are proxy registered in current thread,
     * {@link Optional#empty()} otherwise
     */
    public static Optional<IProxy> getProxy() {
        return getProxy(Thread.currentThread().getId());
    }

    /**
     * Get proxy of specified thread
     *
     * @param threadId thread id
     * @return {@link Optional} of {@link IProxy} if there are proxy registered in specified thread,
     * {@link Optional#empty()} otherwise
     */
    public static Optional<IProxy> getProxy(Long threadId) {
        IProxy proxy = null;
        if (PROXY_POOL.containsKey(threadId)) {
            proxy = PROXY_POOL.get(threadId);
        }
        return Optional.ofNullable(proxy);
    }

    /**
     * Get original proxy (for example, {@link com.zebrunner.carina.proxy.browserup.CarinaBrowserUpProxy}
     *
     * @return {@link Optional} of original proxy if it exists in current thread, {@link Optional#empty()} otherwise
     * @throws ClassCastException if provided class is not a class of original proxy object in current thread
     */
    public static <T> Optional<T> getOriginal(Class<T> clazz) {
        T originalProxyObject = null;

        Optional<IProxy> optionalProxy = getProxy();
        if (optionalProxy.isPresent()) {
            originalProxyObject = (T) clazz.cast(optionalProxy.get());
        }
        return Optional.ofNullable(originalProxyObject);
    }

    /**
     * Stop proxy in current thread
     */
    public static void stopProxy() {
        stopProxy(Thread.currentThread().getId());
    }

    /**
     * Stop proxy in specified thread
     *
     * @param threadId thread id
     */
    public static void stopProxy(Long threadId) {
        if (!PROXY_POOL.containsKey(threadId)) {
            LOGGER.warn("There are no registered Proxy in '{}' thread", threadId);
            return;
        }
        PROXY_POOL.get(threadId).stop();
        setProxyPortToAvailable(threadId);
        PROXY_POOL.remove(threadId);
    }

    /**
     * Stop all proxies in all threads
     */
    public static void stopAllProxies() {
        for (Long threadId : new ArrayList<>(PROXY_POOL.keySet())) {
            stopProxy(threadId);
        }
    }

    /**
     * Get range of ports from proxy_ports configuration parameter
     */
    private static void initProxyPortsRange() {
        // todo add opportunity to set something like 20, 30:40, 400
        if (!getConfigurationParam("proxy_ports").isEmpty()) {
            try {
                String[] ports = getConfigurationParam("proxy_ports").split(":");
                for (int i = Integer.parseInt(ports[0]); i <= Integer.parseInt(ports[1]); i++) {
                    PROXY_PORTS_FROM_RANGE.put(i, true);
                }
            } catch (Exception e) {
                throw new InvalidConfigurationException("Please specify 'proxy_ports' in format 'port_from:port_to'");
            }
        }
    }

    /**
     * Set proxy port status, cached in current thread, as available
     * If port was get from 'proxy_port', it will be just removed from thread-cached port.<br>
     * If port was get from 'proxy_ports', it's status will be changed to available and removed from thread-cached port.
     *
     * @param threadId thread id
     */
    private static void setProxyPortToAvailable(Long threadId) {
        if (PROXY_PORTS_BY_THREAD.get(threadId) != null &&
                PROXY_PORTS_FROM_RANGE.get(PROXY_PORTS_BY_THREAD.get(threadId)) != null) {
            LOGGER.info("Setting proxy port '{}' to available state - means that it will be removed from thread cache "
                            + "and it's status will be set as available if it was got from 'proxy_ports'",
                    PROXY_PORTS_BY_THREAD.get(threadId));
            PROXY_PORTS_FROM_RANGE.put(PROXY_PORTS_BY_THREAD.get(threadId), true);
            PROXY_PORTS_BY_THREAD.remove(threadId);
        }
    }

    /**
     * Get port from configuration.
     *
     * @return 'proxy_port' value from configuration if it exists, proxy_ports available port otherwise
     */
    private static Integer getProxyPortFromConfig() {
        if (!getConfigurationParam("proxy_port").isEmpty()) {
            return Integer.valueOf(getConfigurationParam("proxy_port"));
        } else if (!getConfigurationParam("proxy_ports").isEmpty()) {
            for (Map.Entry<Integer, Boolean> pair : PROXY_PORTS_FROM_RANGE.entrySet()) {
                if (pair.getValue()) {
                    LOGGER.info("Making proxy port busy: {}", pair.getKey());
                    pair.setValue(false);
                    return pair.getKey();
                }
            }
            throw new RuntimeException("All ports from 'proxy_ports' are currently busy. Please change execution thread count");
        }
        throw new RuntimeException("Neither 'proxy_port' nor 'proxy_ports' are specified!");
    }

    //todo replace it when params will be added to the Configuration class
    private static String getConfigurationParam(String param) {
        String value = R.CONFIG.get(param);
        return !(value == null || value.equalsIgnoreCase(SpecialKeywords.NULL)) ? value : StringUtils.EMPTY;
    }

}
