package com.smartheritage.beacon;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * beacon-service (Khoa - Sprint 4): iBeacon CRUD, registry cho Mobile quét BLE, gán hiện vật.
 * Cổng 8084, schema beacon_db. Client gọi qua Gateway http://localhost:8080.
 */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class BeaconServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeaconServiceApplication.class, args);
    }
}
