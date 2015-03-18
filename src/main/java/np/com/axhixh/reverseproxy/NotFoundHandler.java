package np.com.axhixh.reverseproxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

/**
 * Created by ashish on 18/03/15.
 */
public class NotFoundHandler implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        httpServerExchange.setResponseCode(StatusCodes.NOT_FOUND);
        httpServerExchange.getResponseSender().send("Did not find " + httpServerExchange.getRequestURL());
    }
}
