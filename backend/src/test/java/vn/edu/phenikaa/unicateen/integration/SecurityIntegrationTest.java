package vn.edu.phenikaa.unicateen.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** TC-AUTH: phan quyen kiem tra o may chu khi goi truc tiep API (NFR04, BR17, BR42, BR43). */
@AutoConfigureMockMvc
class SecurityIntegrationTest extends IntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void unauthenticatedCallGets401() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void cashierCannotChangePrice_NFR04() throws Exception {
        mvc.perform(post("/api/catalog/menu-items/1/prices").with(as(CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\": 1000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanChangePrice_andItIsAudited_BR06() throws Exception {
        mvc.perform(post("/api/catalog/menu-items/5/prices").with(as(MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\": 4000, \"reason\": \"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(4000));
        org.junit.jupiter.api.Assertions.assertTrue(
                count("SELECT COUNT(*) FROM audit_logs WHERE action = 'PRICE_CHANGE' AND entity_id = 5") >= 1);
    }

    @Test
    void inventoryStaffCannotAdjustStock_BR37() throws Exception {
        mvc.perform(post("/api/stock/adjustments").with(as(INVENTORY)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lotId\": 1, \"actualQty\": 1, \"reason\": \"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentSeesOnlyOwnHistory_BR17() throws Exception {
        mvc.perform(get("/api/orders/mine").with(as(STUDENT))).andExpect(status().isOk());
        mvc.perform(get("/api/orders").with(as(STUDENT))).andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotViewAnotherCashiersOrder_BR17() throws Exception {
        String body = mvc.perform(post("/api/orders").with(as(CASHIER2)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lines\": [{\"menuItemId\": 5, \"quantity\": 1}]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String code = body.replaceAll(".*\"code\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/orders/" + code).with(as(CASHIER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/" + code).with(as(MANAGER))).andExpect(status().isOk());
    }

    @Test
    void businessRuleViolationReturns409WithRuleCode() throws Exception {
        String body = mvc.perform(post("/api/orders").with(as(CASHIER)).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andReturn().getResponse().getContentAsString();
        String code = body.replaceAll(".*\"code\":\"([^\"]+)\".*", "$1");
        mvc.perform(post("/api/orders/" + code + "/submit").with(as(CASHIER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BR12"));
    }

    @Test
    void invalidQuantityReturns400_DV03() throws Exception {
        mvc.perform(post("/api/orders").with(as(CASHIER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\": [{\"menuItemId\": 1, \"quantity\": 100}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fiveWrongPasswordsLockLogin_BR43() throws Exception {
        String wrong = "{\"email\": \"banhang2@phenikaa-uni.edu.vn\", \"password\": \"sai-mat-khau-1\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        String right = "{\"email\": \"banhang2@phenikaa-uni.edu.vn\", \"password\": \"Demo@2026\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(right))
                .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE users SET locked_until = NULL, failed_attempts = 0 WHERE id = 4");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(right))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CASHIER"));
    }

    @Test
    void auditLogHasNoUpdateOrDeleteApi_NFR09() throws Exception {
        mvc.perform(get("/api/accounts/audit-logs").with(as(ADMIN))).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/accounts/audit-logs/1").with(as(ADMIN)))
                .andExpect(status().is4xxClientError());
    }
}
