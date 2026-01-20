package com.keyless.rexroth;

import com.datix.coresystem_poc.CoresystemPocApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyless.rexroth.dto.RCUAssignDTO;
import com.keyless.rexroth.dto.RCUEreignisDTO;
import com.keyless.rexroth.dto.RCURegistrationDTO;
import com.keyless.rexroth.dto.SmartphoneBlockDTO;
import com.keyless.rexroth.dto.SmartphoneRegistrationDTO;
import com.keyless.rexroth.entity.Anomaly;
import com.keyless.rexroth.entity.RCU;
import com.keyless.rexroth.entity.Smartphone;
import com.keyless.rexroth.repository.AnomalyRepository;
import com.keyless.rexroth.repository.EventRepository;
import com.keyless.rexroth.repository.RCURepository;
import com.keyless.rexroth.repository.SmartphoneRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoresystemPocApplication.class)
@AutoConfigureMockMvc
class EventLoggingAnomalyProtectionIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RCURepository rcuRepository;

    @Autowired
    private SmartphoneRepository smartphoneRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @BeforeEach
    void resetDatabase() {
        anomalyRepository.deleteAll();
        eventRepository.deleteAll();
        rcuRepository.deleteAll();
        smartphoneRepository.deleteAll();
    }

    @Test
    void logsEventsDetectsAnomalyAndBlocksDevice() throws Exception {
        RCURegistrationDTO rcuRegistration = new RCURegistrationDTO();
        rcuRegistration.setRcuId("rcu-99");
        rcuRegistration.setName("Validation Cell");
        rcuRegistration.setLocation("Plant Z");

        mockMvc.perform(post("/rcu/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rcuRegistration)))
                .andExpect(status().isOk());

        SmartphoneRegistrationDTO phoneRegistration = new SmartphoneRegistrationDTO();
        phoneRegistration.setDeviceId("device-99");
        phoneRegistration.setName("Validation Phone");

        mockMvc.perform(post("/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phoneRegistration)))
                .andExpect(status().isOk());

        RCU savedRcu = rcuRepository.findByRcuId("rcu-99");
        Smartphone savedPhone = smartphoneRepository.findByDeviceId("device-99");

        RCUAssignDTO assignDTO = new RCUAssignDTO();
        assignDTO.setRcuId(savedRcu.getId());
        assignDTO.setSmartphoneIds(List.of(savedPhone.getId()));

        mockMvc.perform(post("/rcu/assign/smartphones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignDTO)))
                .andExpect(status().isOk());

        RCUEreignisDTO deniedFirst = new RCUEreignisDTO();
        deniedFirst.setRcuId("rcu-99");
        deniedFirst.setDeviceName("Validation User");
        deniedFirst.setDeviceId("device-99");
        deniedFirst.setResult("Zugang verweigert");

        mockMvc.perform(post("/rcu/events/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deniedFirst)))
                .andExpect(status().isNoContent());

        RCUEreignisDTO deniedSecond = new RCUEreignisDTO();
        deniedSecond.setRcuId("rcu-99");
        deniedSecond.setDeviceName("Validation User");
        deniedSecond.setDeviceId("device-99");
        deniedSecond.setResult("Zugang verweigert");

        mockMvc.perform(post("/rcu/events/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deniedSecond)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/rcu/events/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].rcuId", hasItem("rcu-99")))
                .andExpect(jsonPath("$[*].deviceId", hasItem("device-99")))
                .andExpect(jsonPath("$[*].deviceName", hasItem("Validation User")))
                .andExpect(jsonPath("$[*].result", hasItem("Zugang verweigert")))
                .andExpect(jsonPath("$[0].eventTime", not(nullValue())));

        mockMvc.perform(get("/rcu/events/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId", is("device-99")))
                .andExpect(jsonPath("$[0].rcuId", is("rcu-99")))
                .andExpect(jsonPath("$[0].status", is(true)))
                .andExpect(jsonPath("$[0].eventTime", not(nullValue())));

        SmartphoneBlockDTO blockDTO = new SmartphoneBlockDTO();
        blockDTO.setdeviceId("device-99");

        mockMvc.perform(post("/devices/block/smartphone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockDTO)))
                .andExpect(status().isNoContent());

        Smartphone blockedPhone = smartphoneRepository.findByDeviceId("device-99");
        List<Anomaly> anomalies = anomalyRepository.findAllByDeviceId("device-99");

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals("gesperrt", blockedPhone.getStatus()),
                () -> org.junit.jupiter.api.Assertions.assertFalse(anomalies.isEmpty()),
                () -> org.junit.jupiter.api.Assertions.assertTrue(anomalies.stream().allMatch(anomaly -> !anomaly.getStatus()))
        );
    }
}