package com.smartheritage.feedback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RestClient gọi sang service khác, timeout kết nối và đọc theo app.clients.timeout (mặc định 2 giây),
 * tổng thời gian chờ tối đa theo app.clients.deadline (mặc định 1,5 giây, TC-46).
 * Quá thời gian hoặc service kia tắt thì client ném lỗi, xem ArtifactClient / UserProfileClient.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient contentRestClient(RestClient.Builder builder,
                                        @Value("${app.clients.content-url}") String baseUrl,
                                        @Value("${app.clients.timeout:2s}") Duration timeout,
                                        @Value("${app.clients.deadline:1500ms}") Duration deadline) {
        return create(builder, baseUrl, timeout, deadline);
    }

    @Bean
    public RestClient authRestClient(RestClient.Builder builder,
                                     @Value("${app.clients.auth-url}") String baseUrl,
                                     @Value("${app.clients.timeout:2s}") Duration timeout,
                                     @Value("${app.clients.deadline:1500ms}") Duration deadline) {
        return create(builder, baseUrl, timeout, deadline);
    }

    private static RestClient create(RestClient.Builder builder, String baseUrl, Duration timeout, Duration deadline) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        // clone(): mỗi client một builder riêng, không đè cấu hình của nhau
        return builder.clone().baseUrl(baseUrl).requestFactory(factory)
                .requestInterceptor(new DeadlineInterceptor(deadline))
                .build();
    }

    /**
     * TC-46: giới hạn TỔNG thời gian chờ service khác, gồm cả phân giải tên container.
     * connect/read timeout của HttpURLConnection không tính bước tra DNS: container vừa tắt thì tên không còn
     * trong DNS của Docker, việc tra tên có thể mất ~2 s trước khi đếm connect timeout -> 503 sau ~4 s.
     * Interceptor chạy lời gọi trên luồng phụ và bỏ chờ khi quá hạn -> lỗi ResourceAccessException,
     * client hiện có bắt RestClientException và trả 503 như cũ.
     */
    public static final class DeadlineInterceptor implements ClientHttpRequestInterceptor {

        private static final AtomicInteger THREAD_SEQ = new AtomicInteger();
        /** Tối đa 64 lời gọi chờ cùng lúc; vượt thì báo lỗi ngay (503) thay vì xếp hàng. */
        private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
                0, 64, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), runnable -> {
                    Thread thread = new Thread(runnable, "client-deadline-" + THREAD_SEQ.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });

        private final Duration deadline;

        public DeadlineInterceptor(Duration deadline) {
            if (deadline == null || deadline.isZero() || deadline.isNegative()) {
                throw new IllegalArgumentException("deadline phải > 0");
            }
            this.deadline = deadline;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            Future<ClientHttpResponse> future;
            try {
                future = EXECUTOR.submit(() -> execution.execute(request, body));
            } catch (RejectedExecutionException e) {
                throw new ResourceAccessException("Quá nhiều lời gọi đang chờ " + request.getURI().getHost());
            }
            try {
                return future.get(deadline.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new ResourceAccessException("Quá " + deadline.toMillis() + " ms khi gọi " + request.getURI());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                throw new ResourceAccessException("Bị ngắt khi gọi " + request.getURI());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new ResourceAccessException("Lỗi khi gọi " + request.getURI() + ": " + cause);
            }
        }
    }
}
