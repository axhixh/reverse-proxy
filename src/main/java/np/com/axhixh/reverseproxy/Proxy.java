package np.com.axhixh.reverseproxy;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.predicate.PredicatesHandler;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by ashish on 17/03/15.
 */
public class Proxy {

    private PredicatesHandler router;
    private Undertow server;

    public Proxy(HttpHandler nextHandler) {
        this.router = new PredicatesHandler(nextHandler);
    }

    public void addRoute(String path, String... backends) throws URISyntaxException {
        router.addPredicatedHandler(Predicates.prefix(path), setupLoadBalancer(backends));
    }

    public void start(int port, String address) {
        if (server != null) {
            server.stop();
        }
        server = Undertow.builder()
                .addHttpListener(port, address)
                .setIoThreads(5)
                .setHandler(router)
                .build();
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public static void main(String[] args) throws URISyntaxException {
        HttpHandler notFoundHandler = null;
        Proxy proxy = new Proxy(notFoundHandler);
        proxy.addRoute("/api-1", "http://localhost:8090/api-1", "http://localhost:8091/api-1");
        proxy.addRoute("/api-2", "http://localhost:8095/api-2", "http://localhost:8096/api-2");
        proxy.start(8080, "0.0.0.0");
    }

    private static HandlerWrapper setupLoadBalancer(String... backends) throws URISyntaxException {
        final LoadBalancingProxyClient client = new LoadBalancingProxyClient();
        client.setConnectionsPerThread(5);

        for (String backend : backends) {
            client.addHost(new URI(backend));
        }

        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler httpHandler) {
                return Handlers.proxyHandler(client, httpHandler);
            }
        };
    }
}
