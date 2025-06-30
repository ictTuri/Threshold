package al.lhind.tab.claim.controller;

import al.lhind.tab.claim.aspect.RateLimitAspect;
import al.lhind.tab.claim.config.ThresholdFilter;
import al.lhind.tab.claim.model.ui.ClaimDto;
import al.lhind.tab.claim.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ClaimControllerFilterTest {

    @Value("${rate.limit.capacity}")
    private Integer rateLimit;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RateLimitAspect rateLimitAspect;

    @MockBean
    private ClaimService claimService;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitAspect.clearBuckets();
        Mockito.when(claimService.getClaim(Mockito.anyLong()))
                .thenReturn(new ClaimDto());
    }

    @Test
    void givenApi_whenTriedManyTime_thenValidateThresholdIsMeet() throws Exception {
        for (int i = 0; i < rateLimit; i++) {
            mockMvc.perform(get("/claims/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
        // The next request should be rate limited
        mockMvc.perform(get("/claims/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());
    }
}
