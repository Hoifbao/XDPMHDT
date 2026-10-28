package com.smartheritage.content.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
@Component
public class SiteClient {
 private final RestClient client;
 public SiteClient(@Value("${services.site.url}") String url) {
  var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
  factory.setReadTimeout(Duration.ofSeconds(5));
  client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
 }
 public record Site(Long id, String name) {}
 public Site requireSite(Long id) {
  if (id == null || id <= 0) throw new IllegalArgumentException("siteId is required");
  try {
   Site site = client.get().uri("/api/heritage-sites/{id}", id).retrieve().body(Site.class);
   if (site == null || !id.equals(site.id())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Invalid site-service response");
   return site;
  } catch (RestClientResponseException e) {
   if (e.getStatusCode().value() == 404) throw new IllegalArgumentException("Unknown siteId: " + id);
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"site-service unavailable",e);
  } catch (ResourceAccessException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"site-service unavailable",e); }
 }
}
