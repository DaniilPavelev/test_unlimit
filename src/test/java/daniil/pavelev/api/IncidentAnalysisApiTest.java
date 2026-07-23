package daniil.pavelev.api;

import daniil.pavelev.support.StubLlmClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StubLlmClientConfig.class)
class IncidentAnalysisApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postValidRequestReturns201AndLocation() throws Exception {
        mockMvc.perform(post("/api/v1/incident-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Customers cannot pay by card. payment-service logs show PayGate timeouts."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.category").isNotEmpty())
                .andExpect(jsonPath("$.severity").isNotEmpty())
                .andExpect(jsonPath("$.hypotheses").isArray());
    }

    @Test
    void blankDescriptionReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/incident-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExistingAndUnknownAnalysis() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/incident-analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"auth-service returns HTTP 401 with invalid token signature errors."}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/incident-analyses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/v1/incident-analyses/00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound());
    }

    @Test
    void homePageIsAvailable() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
