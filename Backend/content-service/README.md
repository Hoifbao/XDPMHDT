# content-service

Spring Boot 3 / Java 17. Owns artifacts, translations and media in `content_db`.
Runs on port 8083. `SiteClient` validates scalar siteId via site-service HTTP;
there are no cross-database JPA relations.

```powershell
mvn -f Backend/content-service/pom.xml test
mvn -f Backend/content-service/pom.xml spring-boot:run
```

Configuration: `SERVER_PORT`, `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SITE_SERVICE_URL`.

See [API, CMS, Docker setup and sample JSON](../../Docs/api/artifacts-localization.md).
