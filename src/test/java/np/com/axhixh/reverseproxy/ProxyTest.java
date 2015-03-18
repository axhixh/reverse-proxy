package np.com.axhixh.reverseproxy;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by ashish on 17/03/15.
 */
public class ProxyTest {

    private Proxy proxy;

    private Undertow[] backends;

    @Before
    public void setup() throws Exception {
        backends = new Undertow[4];
        backends[0] = createBackend(8090, "/api-1");
        backends[1] = createBackend(8091, "/api-1");
        backends[2] = createBackend(8095, "/api-2");
        backends[3] = createBackend(8096, "/api-2");

        for (Undertow backend : backends) {
            backend.start();
        }

        HttpHandler notFoundHandler = new NotFoundHandler();
        proxy = new Proxy(notFoundHandler);
        proxy.addRoute("/api-1", "http://localhost:8090", "http://localhost:8091");
        proxy.addRoute("/api-2", "http://localhost:8095", "http://localhost:8096");
        proxy.start(8080, "0.0.0.0");
    }

    private Undertow createBackend(final int port, final String route) throws Exception {
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("Hello from " + route + "@" + port + " path is: " + exchange.getRequestURL());
                    }
                })
                .build();
        return server;
    }

    @After
    public void teardown() {
        proxy.stop();
        for (Undertow backend : backends) {
            backend.stop();
        }
    }

    @Test
    public void testThatProxyCallsCorrectBackend() throws Exception {
        System.out.println("test for correct backend");
        for (int i = 0; i < 5; i++) {
            String body = get("http://localhost:8080/api-1");
            System.out.println(body);
            Assert.assertTrue(body.startsWith("Hello from /api-1@"));
        }
        for (int i = 0; i < 5; i++) {
            String body = get("http://localhost:8080/api-2");
            System.out.println(body);
            Assert.assertTrue(body.startsWith("Hello from /api-2@"));
        }
    }

    @Test
    public void testThatProxyPassesFullPath() throws Exception {
        System.out.println("test for full path");
        String body = get("http://localhost:8080/api-1/hello/test");
        System.out.println(body);
        Assert.assertTrue(body.endsWith("/api-1/hello/test"));

    }

    @Test(expected = FileNotFoundException.class)
    public void testThatProxyReturnsNotFoundForMissingUrl() throws Exception {
        get("http://localhost:8080/api-3");
    }

    private String get(String url) throws IOException {
        URL remote = new URL(url);
        try (InputStream in = remote.openStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toString("UTF-8");
        }
    }
}
