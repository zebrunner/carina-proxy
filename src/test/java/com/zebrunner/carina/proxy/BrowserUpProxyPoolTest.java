/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.zebrunner.carina.proxy;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.proxy.CaptureType;
import com.zebrunner.carina.proxy.browserup.CarinaBrowserUpProxy;
import com.zebrunner.carina.proxy.browserup.LocalTrustStoreBuilder;
import com.zebrunner.carina.utils.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// todo investigate freeze when building a project in a snapshot
@Test(enabled = false)
public class BrowserUpProxyPoolTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static String header = "my_header";
    private static String headerValue = "my_value";
    private static String testUrl = "https://ci.qaprosoft.com";
    private static String filterKey = "</html>";
    private static String requestMethod = "GET";

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        ProxyPool.setRule(new DefaultProxyRule(), true);
        R.CONFIG.put("core_log_level", "DEBUG");
        R.CONFIG.put("browserup_proxy", "true");
        R.CONFIG.put("proxy_set_to_system", "true");
        R.CONFIG.put("browserup_disabled_mitm", "false");
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        ProxyPool.stopAllProxies();
    }

    @Test
    public void testIsBrowserUpStarted() {
        initialize();
        Optional<IProxy> proxy = ProxyPool.getProxy();
        Assert.assertTrue(proxy.isPresent(), "Proxy should present in the pool.");
        Assert.assertTrue(proxy.get().isStarted(), "BrowserUpProxy is not started!");
    }

    @Test
    public void testBrowserUpProxySystemIntegration() {
        initialize();
        IProxyInfo proxyInfo = ProxyPool.getProxy()
                .orElseThrow()
                .getInfo();
        Assert.assertEquals(proxyInfo.getHost(), System.getProperty("http.proxyHost"));
        Assert.assertEquals(String.valueOf(proxyInfo.getPort()), System.getProperty("http.proxyPort"));
    }

    @Test
    public void testBrowserUpProxyHeader() {
        initialize();
        BrowserUpProxy browserUpProxy = ProxyPool.getOriginal(CarinaBrowserUpProxy.class)
                .orElseThrow(() -> new RuntimeException("Proxy should be registered")).getProxy();
        Map<String, String> headers = browserUpProxy.getAllHeaders();
        Assert.assertTrue(headers.containsKey(header), "There is no custom header: " + header);
        Assert.assertTrue(headers.get(header).equals(headerValue), "There is no custom header value: " + headerValue);

        browserUpProxy.removeHeader(header);
        if (browserUpProxy.getAllHeaders().size() != 0) {
            Assert.fail("Custom header was not removed: " + header);
        }
    }

    @Test
    public void testBrowserUpProxyRegistration() {
        ProxyPool.startProxy();
        ProxyPool.register(ProxyPool.getProxy().orElseThrow());
        Assert.assertTrue(ProxyPool.isProxyRegistered(), "Proxy wasn't registered in ProxyPool!");
        ProxyPool.stopAllProxies();
        Assert.assertFalse(ProxyPool.isProxyRegistered(), "Proxy wasn't stopped!");
    }

    @Test
    public void testBrowserUpProxyResponseFiltering() {
        List<String> content = new ArrayList<>();
        LocalTrustStoreBuilder localTrustStoreBuilder = new LocalTrustStoreBuilder();
        SSLContext sslContext = localTrustStoreBuilder.createSSLContext();
        SSLContext.setDefault(sslContext);

        ProxyPool.startProxy();
        SystemProxy.setupProxy();

        BrowserUpProxy proxy = ProxyPool.getOriginal(CarinaBrowserUpProxy.class)
                .orElseThrow(() -> new RuntimeException("There should be BrowserUp original proxy object"))
                .getProxy();
        proxy.enableHarCaptureTypes(CaptureType.RESPONSE_CONTENT);
        proxy.newHar();

        proxy.addResponseFilter((request, contents, messageInfo) -> {
            LOGGER.info("Requested resource caught contents: " + contents.getTextContents());
            if (contents.getTextContents().contains(filterKey)) {
                content.add(contents.getTextContents());
            }
        });

        makeHttpRequest(testUrl, requestMethod);

        Assert.assertNotNull(proxy.getHar(), "Har is unexpectedly null!");
        Assert.assertEquals(content.size(), 1, "Filtered response number is not as expected!");
        Assert.assertTrue(content.get(0).contains(filterKey), "Response doesn't contain expected key!");
    }

    @DataProvider(parallel = false)
    public static Object[][] dataProviderForMultiThreadProxy() {
        return new Object[][] {
                { "Test1" },
                { "Test2" } };
    }

    @Test(dataProvider = "dataProviderForMultiThreadProxy")
    public void testRegisterProxy(String arg) {
        ProxyPool.startProxy();
        IProxyInfo proxyInfo = ProxyPool.getProxy().orElseThrow()
                .getInfo();
        int tempPort = proxyInfo.getPort();
        CarinaBrowserUpProxy carinaBrowserUpProxy = new CarinaBrowserUpProxy();
        BrowserUpProxy proxy = carinaBrowserUpProxy.getProxy();
        proxy.setTrustAllServers(true);
        proxy.setMitmDisabled(false);
        ProxyPool.register(carinaBrowserUpProxy);

        ProxyPool.startProxy();
        int actualPort = ProxyPool.getOriginal(CarinaBrowserUpProxy.class).orElseThrow().getProxy().getPort();
        LOGGER.info(String.format("Checking Ports Before (%s) After (%s)", tempPort, actualPort));
        Assert.assertEquals(tempPort, actualPort, "Proxy Port before, after do not match on current thread");
    }

    private void initialize() {
        ProxyPool.startProxy();
        SystemProxy.setupProxy();
        BrowserUpProxy proxy = ProxyPool.getOriginal(CarinaBrowserUpProxy.class).orElseThrow().getProxy();
        proxy.addHeader(header, headerValue);
    }

    private void makeHttpRequest(String requestUrl, String requestMethod) {
        URL url;
        HttpURLConnection con;
        Integer httpResponseStatus;
        try {
            url = new URL(requestUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(requestMethod);
            httpResponseStatus = con.getResponseCode();
            LOGGER.info("httpResponseStatus" + httpResponseStatus);
            Assert.assertTrue(httpResponseStatus < 399, "Response code is not as expected!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
