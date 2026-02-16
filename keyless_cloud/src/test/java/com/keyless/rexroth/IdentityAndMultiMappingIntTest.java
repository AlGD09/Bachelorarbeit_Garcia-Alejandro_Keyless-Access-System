package com.keyless.rexroth;

import com.datix.coresystem_poc.CoresystemPocApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyless.rexroth.dto.RCUAssignDTO;
import com.keyless.rexroth.dto.RCURegistrationDTO;
import com.keyless.rexroth.dto.SmartphoneAssignDTO;
import com.keyless.rexroth.dto.SmartphoneRegistrationDTO;
import com.keyless.rexroth.dto.SmartphoneTokenDTO;
import com.keyless.rexroth.dto.UserRegistrationDTO;
import com.keyless.rexroth.dto.UserUnassignDTO;
import com.keyless.rexroth.entity.RCU;
import com.keyless.rexroth.entity.Smartphone;
import com.keyless.rexroth.entity.User;
import com.keyless.rexroth.repository.RCURepository;
import com.keyless.rexroth.repository.SmartphoneRepository;
import com.keyless.rexroth.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoresystemPocApplication.class)
@AutoConfigureMockMvc
class IdentityAndMultiMappingIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmartphoneRepository smartphoneRepository;

    @Autowired
    private RCURepository rcuRepository;

    @BeforeEach
    void resetDatabase() {
        rcuRepository.deleteAll();
        smartphoneRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void identityAndMultiMappingLifecycle() throws Exception {
        UserRegistrationDTO userOne = new UserRegistrationDTO();
        userOne.setUsername("user-one");
        userOne.setEmail("user-one@example.com");
        userOne.setSecretHash("hash-one");

        UserRegistrationDTO userTwo = new UserRegistrationDTO();
        userTwo.setUsername("user-two");
        userTwo.setEmail("user-two@example.com");
        userTwo.setSecretHash("hash-two");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userOne)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userTwo)))
                .andExpect(status().isOk());

        SmartphoneRegistrationDTO smartphoneOne = new SmartphoneRegistrationDTO();
        smartphoneOne.setDeviceId("smph-01");
        smartphoneOne.setName("Phone One");

        SmartphoneRegistrationDTO smartphoneTwo = new SmartphoneRegistrationDTO();
        smartphoneTwo.setDeviceId("smph-02");
        smartphoneTwo.setName("Phone Two");

        mockMvc.perform(post("/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smartphoneOne)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smartphoneTwo)))
                .andExpect(status().isOk());

        RCURegistrationDTO rcuOne = new RCURegistrationDTO();
        rcuOne.setRcuId("rcu-01");
        rcuOne.setName("Machine One");
        rcuOne.setLocation("Plant A");

        RCURegistrationDTO rcuTwo = new RCURegistrationDTO();
        rcuTwo.setRcuId("rcu-02");
        rcuTwo.setName("Machine Two");
        rcuTwo.setLocation("Plant B");

        mockMvc.perform(post("/rcu/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rcuOne)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rcu/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rcuTwo)))
                .andExpect(status().isOk());

        User savedUserOne = userRepository.findByUsername("user-one");
        User savedUserTwo = userRepository.findByUsername("user-two");
        Smartphone savedSmartphoneOne = smartphoneRepository.findByDeviceId("smph-01");
        Smartphone savedSmartphoneTwo = smartphoneRepository.findByDeviceId("smph-02");
        RCU savedRcuOne = rcuRepository.findByRcuId("rcu-01");
        RCU savedRcuTwo = rcuRepository.findByRcuId("rcu-02");

        SmartphoneAssignDTO assignUsersToFirstPhone = new SmartphoneAssignDTO();
        assignUsersToFirstPhone.setsmartphoneId(savedSmartphoneOne.getId());
        assignUsersToFirstPhone.setUserIds(List.of(savedUserOne.getId(), savedUserTwo.getId()));

        mockMvc.perform(post("/devices/assign/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignUsersToFirstPhone)))
                .andExpect(status().isOk());

        SmartphoneAssignDTO assignUserToSecondPhone = new SmartphoneAssignDTO();
        assignUserToSecondPhone.setsmartphoneId(savedSmartphoneTwo.getId());
        assignUserToSecondPhone.setUserIds(List.of(savedUserOne.getId()));

        mockMvc.perform(post("/devices/assign/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignUserToSecondPhone)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/devices/" + savedSmartphoneOne.getId() + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("user-one", "user-two")));

        mockMvc.perform(get("/devices/" + savedSmartphoneTwo.getId() + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("user-one"));

        RCUAssignDTO assignPhonesToFirstRcu = new RCUAssignDTO();
        assignPhonesToFirstRcu.setRcuId(savedRcuOne.getId());
        assignPhonesToFirstRcu.setSmartphoneIds(List.of(savedSmartphoneOne.getId(), savedSmartphoneTwo.getId()));

        mockMvc.perform(post("/rcu/assign/smartphones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignPhonesToFirstRcu)))
                .andExpect(status().isOk());

        RCUAssignDTO assignPhoneToSecondRcu = new RCUAssignDTO();
        assignPhoneToSecondRcu.setRcuId(savedRcuTwo.getId());
        assignPhoneToSecondRcu.setSmartphoneIds(List.of(savedSmartphoneOne.getId()));

        mockMvc.perform(post("/rcu/assign/smartphones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignPhoneToSecondRcu)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rcu/" + savedRcuOne.getRcuId() + "/smartphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].deviceId", containsInAnyOrder("smph-01", "smph-02")));

        mockMvc.perform(get("/rcu/" + savedRcuTwo.getRcuId() + "/smartphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceId").value("smph-01"));

        SmartphoneTokenDTO tokenRequest = new SmartphoneTokenDTO();
        tokenRequest.setDeviceId(savedSmartphoneOne.getDeviceId());
        tokenRequest.setUserName(savedUserOne.getUsername());
        tokenRequest.setSecretHash("hash-one");

        mockMvc.perform(post("/devices/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth_token").isNotEmpty());

        UserUnassignDTO unassignUser = new UserUnassignDTO();
        unassignUser.setSmartphoneId(savedSmartphoneOne.getDeviceId());
        unassignUser.setUserId(savedUserOne.getId());

        mockMvc.perform(post("/users/remove/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unassignUser)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/devices/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }
}