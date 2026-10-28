package com.smartheritage.content;

import com.smartheritage.content.service.SiteClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SiteClientTest {
 @Test void validatesRemoteSiteAndMapsRemoteFailures() throws Exception {
  HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  server.createContext("/api/heritage-sites/", exchange -> {
   String path = exchange.getRequestURI().getPath();
   int code = path.endsWith("/1") ? 200 : path.endsWith("/2") ? 404 : 500;
   byte[] body = "{\"id\":1,\"name\":\"Museum\"}".getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().set("Content-Type", "application/json");
   exchange.sendResponseHeaders(code, body.length);
   try (var stream = exchange.getResponseBody()) { stream.write(body); }
  });
  server.start();
  try {
   var client = new SiteClient("http://127.0.0.1:" + server.getAddress().getPort());
   assertEquals("Museum", client.requireSite(1L).name());
   assertThrows(IllegalArgumentException.class, () -> client.requireSite(null));
   assertThrows(IllegalArgumentException.class, () -> client.requireSite(2L));
   assertEquals(503, assertThrows(ResponseStatusException.class, () -> client.requireSite(3L)).getStatusCode().value());
  } finally { server.stop(0); }
 }
}
