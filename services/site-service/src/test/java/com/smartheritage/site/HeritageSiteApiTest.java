package com.smartheritage.site;

import com.smartheritage.site.entity.HeritageSite;
import com.smartheritage.site.repository.HeritageSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test API di tích trên H2 (chế độ MySQL), không cần bật MySQL.
 * Phủ: CRUD, nearby sắp gần → xa, bỏ di tích ngoài bán kính, tham số sai → 400, id không tồn tại → 404.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:site;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class HeritageSiteApiTest {

    @Autowired MockMvc mvc;
    @Autowired HeritageSiteRepository repository;

    /** Ba điểm đối chiếu tay (đúng như bảng trong PR): 0 km, ~0.86 km, ~41.4 km tính từ Văn Miếu. */
    static final String VAN_MIEU = "Văn Miếu - Quốc Tử Giám";
    static final String HOANG_THANH = "Hoàng thành Thăng Long";
    static final String XA = "Điểm xa (Cầu Giấy)";

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(site(VAN_MIEU, 21.0277644, 105.8354028));
        repository.save(site(HOANG_THANH, 21.0354430, 105.8349380));
        repository.save(site(XA, 21.4000000, 105.8354028));
    }

    // ------------------------------------------------------------------ nearby

    @Test
    void nearby_sortsNearestFirstAndSkipsSitesOutsideRadius() throws Exception {
        mvc.perform(get("/api/heritage-sites/nearby")
                        .param("lat", "21.0277644").param("lng", "105.8354028").param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", contains(VAN_MIEU, HOANG_THANH)))
                .andExpect(jsonPath("$.data[0].distanceKm").value(0.00))
                .andExpect(jsonPath("$.data[1].distanceKm").value(0.86));
    }

    @Test
    void nearby_uses5kmDefaultWhenRadiusIsMissing() throws Exception {
        mvc.perform(get("/api/heritage-sites/nearby")
                        .param("lat", "21.0277644").param("lng", "105.8354028"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 41.4 km chỉ lọt khi nới bán kính lên 50 km (mức tối đa)
        mvc.perform(get("/api/heritage-sites/nearby")
                        .param("lat", "21.0277644").param("lng", "105.8354028").param("radius", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[2].name").value(XA));
    }

    @Test
    void nearby_rejectsInvalidCoordinatesAndRadius() throws Exception {
        mvc.perform(get("/api/heritage-sites/nearby").param("lat", "91").param("lng", "105"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mvc.perform(get("/api/heritage-sites/nearby").param("lat", "21").param("lng", "181"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/heritage-sites/nearby")
                        .param("lat", "21").param("lng", "105").param("radius", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bán kính tìm kiếm tối đa là 50 km."));

        mvc.perform(get("/api/heritage-sites/nearby")
                        .param("lat", "21").param("lng", "105").param("radius", "0"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------- CRUD

    @Test
    void listAndGetById_returnSites() throws Exception {
        mvc.perform(get("/api/heritage-sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        Long id = repository.findByNameContainingIgnoreCase(HOANG_THANH).get(0).getId();
        mvc.perform(get("/api/heritage-sites/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(HOANG_THANH))
                .andExpect(jsonPath("$.data.latitude").value(21.0354430));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mvc.perform(get("/api/heritage-sites/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createUpdateDelete_roundTrip() throws Exception {
        String body = """
                {"name":"Chùa Một Cột","location":"Ba Đình, Hà Nội","latitude":21.0358,"longitude":105.8335}
                """;

        mvc.perform(post("/api/heritage-sites").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name").value("Chùa Một Cột"));

        Long id = repository.findByNameContainingIgnoreCase("Chùa Một Cột").get(0).getId();

        // Kéo marker trên bản đồ rồi lưu: cập nhật tọa độ
        mvc.perform(put("/api/heritage-sites/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":21.0360,\"longitude\":105.8340}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(21.0360))
                .andExpect(jsonPath("$.data.name").value("Chùa Một Cột"));

        // clearCoordinates = true: xóa tọa độ, di tích biến mất khỏi /nearby
        mvc.perform(put("/api/heritage-sites/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearCoordinates\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").doesNotExist());

        mvc.perform(get("/api/heritage-sites/nearby")
                        .param("lat", "21.0358").param("lng", "105.8335").param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", not(hasItem("Chùa Một Cột"))));

        mvc.perform(delete("/api/heritage-sites/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(get("/api/heritage-sites/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void create_rejectsBlankNameAndHalfCoordinates() throws Exception {
        mvc.perform(post("/api/heritage-sites").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/heritage-sites").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thiếu kinh độ\",\"latitude\":21.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vĩ độ và kinh độ phải được cung cấp cùng nhau."));
    }

    @Test
    void create_rejectsDuplicateName() throws Exception {
        mvc.perform(post("/api/heritage-sites").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + VAN_MIEU + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private HeritageSite site(String name, double latitude, double longitude) {
        return HeritageSite.builder()
                .name(name)
                .latitude(BigDecimal.valueOf(latitude))
                .longitude(BigDecimal.valueOf(longitude))
                .build();
    }
}
