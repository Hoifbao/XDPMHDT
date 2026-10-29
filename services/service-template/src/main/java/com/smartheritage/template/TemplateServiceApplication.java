package com.smartheritage.template;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * MẪU service (Bảo - Sprint 4). Khi copy: đổi package com.smartheritage.template
 * thành com.smartheritage.&lt;tên&gt; (vd com.smartheritage.site) và đổi tên class này (vd SiteServiceApplication).
 * Giữ @Import(GlobalExceptionHandler.class) để lỗi trả JSON thống nhất.
 */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class TemplateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateServiceApplication.class, args);
    }
}
