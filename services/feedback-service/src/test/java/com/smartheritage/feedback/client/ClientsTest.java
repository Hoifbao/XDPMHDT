package com.smartheritage.feedback.client;

import com.smartheritage.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Kiểm tra ArtifactClient và UserProfileClient với HTTP giả (MockRestServiceServer). */
class ClientsTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ─────────────── ArtifactClient ───────────────

    @Test
    void artifactClient_200_docStatus() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://content");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://content/api/artifacts/1"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"id\":1,\"status\":\"ACTIVE\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(new ArtifactClient(builder.build()).getArtifactStatus(1L)).isEqualTo("ACTIVE");
        server.verify();
    }

    @Test
    void artifactClient_404_traApiException404() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://content");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://content/api/artifacts/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> new ArtifactClient(builder.build()).getArtifactStatus(99L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void artifactClient_500_traApiException503() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://content");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://content/api/artifacts/1")).andRespond(withServerError());

        assertThatThrownBy(() -> new ArtifactClient(builder.build()).getArtifactStatus(1L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void artifactClient_khongKetNoiDuoc_traApiException503() {
        // Cổng 1 không có service nào nghe -> từ chối kết nối ngay
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:1").requestFactory(factory).build();

        assertThatThrownBy(() -> new ArtifactClient(client).getArtifactStatus(1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("content-service")
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // ─────────────── UserProfileClient ───────────────

    @Test
    void userProfileClient_chuyenTiepHeaderVaDocFullName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "2");
        request.addHeader("X-User-Name", "visitor_an");
        request.addHeader("X-User-Role", "VISITOR");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/auth/me"))
                .andExpect(header("X-User-Id", "2"))
                .andExpect(header("X-User-Role", "VISITOR"))
                .andRespond(withSuccess("{\"userId\":2,\"fullName\":\" Nguyễn Văn An \"}", MediaType.APPLICATION_JSON));

        assertThat(new UserProfileClient(builder.build()).findCurrentUserFullName()).contains("Nguyễn Văn An");
        server.verify();
    }

    @Test
    void userProfileClient_authLoiHoacTenRong_traEmpty() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://auth/api/auth/me")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("http://auth/api/auth/me"))
                .andRespond(withSuccess("{\"fullName\":\"\"}", MediaType.APPLICATION_JSON));

        UserProfileClient client = new UserProfileClient(builder.build());
        assertThat(client.findCurrentUserFullName()).isEmpty();
        assertThat(client.findCurrentUserFullName()).isEmpty();
    }
}
