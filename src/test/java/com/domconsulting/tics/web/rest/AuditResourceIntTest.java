package com.domconsulting.tics.web.rest;

import com.domconsulting.tics.TicsdemoApp;
import com.domconsulting.tics.config.audit.AuditEventConverter;
import com.domconsulting.tics.domain.PersistentAuditEvent;
import com.domconsulting.tics.repository.PersistenceAuditEventRepository;
import com.domconsulting.tics.service.AuditEventService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the AuditResource REST controller.
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TicsdemoApp.class)
@Transactional
public class AuditResourceIntTest {

    private static final String SAMPLE_PRINCIPAL = "SAMPLE_PRINCIPAL";
    private static final String SAMPLE_TYPE = "SAMPLE_TYPE";
    private static final LocalDateTime SAMPLE_TIMESTAMP = LocalDateTime.parse("2015-08-04T10:11:30");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject
    private PersistenceAuditEventRepository auditEventRepository;

    @Inject
    private AuditEventConverter auditEventConverter;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private FormattingConversionService formattingConversionService;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private PersistentAuditEvent auditEvent;

    private MockMvc restAuditMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        AuditEventService auditEventService =
                new AuditEventService(auditEventRepository, auditEventConverter);
        AuditResource auditResource = new AuditResource(auditEventService);
        this.restAuditMockMvc = MockMvcBuilders.standaloneSetup(auditResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setConversionService(formattingConversionService)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        auditEventRepository.deleteAll();
        auditEvent = new PersistentAuditEvent();
        auditEvent.setAuditEventType(SAMPLE_TYPE);
        auditEvent.setPrincipal(SAMPLE_PRINCIPAL);
        auditEvent.setAuditEventDate(SAMPLE_TIMESTAMP);
    }

    @Test
    public void getAllAudits() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        // Get all the audits
        restAuditMockMvc.perform(get("/management/audits"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].principal").value(hasItem(SAMPLE_PRINCIPAL)));
    }

    @Test
    public void getAudit() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        // Get the audit
        restAuditMockMvc.perform(get("/management/audits/{id}", auditEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.principal").value(SAMPLE_PRINCIPAL));
    }

    @Test
    public void getAuditsByDate() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        // Generate dates for selecting audits by date, making sure the period will contain the audit
        String fromDate  = SAMPLE_TIMESTAMP.minusDays(1).format(FORMATTER);
        String toDate = SAMPLE_TIMESTAMP.plusDays(1).format(FORMATTER);

        // Get the audit
        restAuditMockMvc.perform(get("/management/audits?fromDate="+fromDate+"&toDate="+toDate))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].principal").value(hasItem(SAMPLE_PRINCIPAL)));
    }

    @Test
    public void getNonExistingAuditsByDate() throws Exception {
        // Initialize the database
        auditEventRepository.save(auditEvent);

        // Generate dates for selecting audits by date, making sure the period will not contain the sample audit
        String fromDate  = SAMPLE_TIMESTAMP.minusDays(2).format(FORMATTER);
        String toDate = SAMPLE_TIMESTAMP.minusDays(1).format(FORMATTER);

        // Query audits but expect no results
        restAuditMockMvc.perform(get("/management/audits?fromDate=" + fromDate + "&toDate=" + toDate))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(header().string("X-Total-Count", "0"));
    }

    @Test
    public void getNonExistingAudit() throws Exception {
        // Get the audit
        restAuditMockMvc.perform(get("/management/audits/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

}
