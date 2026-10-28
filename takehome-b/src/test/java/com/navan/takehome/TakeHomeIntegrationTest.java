package com.navan.takehome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.takehome.dto.InvoiceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TakeHomeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testFullWorkflowAndRequirements() throws Exception {
        // 1. Create flight, hotel, rail, and two navan_fee types
        String flightJson = """
            {
              "type": "flight",
              "external_id": "flight-TXL-LHR-2026-03-11",
              "currency": "EUR",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 220.0,
              "tax_lines": [{ "name": "Airport tax", "amount": 15.0 }],
              "fee_lines": [{ "name": "YQ", "amount": 25.0 }],
              "metadata": {
                "origin": "TXL",
                "destination": "LHR",
                "fare": 180.0,
                "coupons": [{ "carrier": "BA", "flight_number": "BA982", "from": "TXL", "to": "LHR", "departure": "2026-03-11T07:00:00Z", "arrival": "2026-03-11T08:20:00Z" }]
              }
            }
            """;

        var flightResult = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(flightJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.metadata.coupons", hasSize(1)))
                .andReturn();

        String flightId = objectMapper.readTree(flightResult.getResponse().getContentAsString()).get("id").asText();

        // Hotel
        String hotelJson = """
            {
              "type": "hotel",
              "external_id": "hotel-LON-2026-03-11",
              "currency": "EUR",
              "occurred_at": "2026-03-11T15:00:00Z",
              "total": 331.7,
              "tax_lines": [{ "name": "VAT", "rate": 0.07, "amount": 21.7 }],
              "fee_lines": [],
              "metadata": {
                "name": "Hotel Mitte London",
                "address": { "street": "12 Kingsway", "city": "London", "country": "GB" },
                "check_in": "2026-03-11",
                "check_out": "2026-03-13",
                "nights": 2,
                "room_rate_per_night": 140.0,
                "extras": [{ "name": "Breakfast", "amount": 30.0 }]
              }
            }
            """;
        var hotelResult = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(hotelJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata.name", is("Hotel Mitte London")))
                .andExpect(jsonPath("$.metadata.address.city", is("London")))
                .andReturn();
        String hotelId = objectMapper.readTree(hotelResult.getResponse().getContentAsString()).get("id").asText();

        // Rail
        String railJson = """
            {
              "type": "rail",
              "external_id": "rail-STP-PAR-2026-03-13",
              "currency": "EUR",
              "occurred_at": "2026-03-13T09:00:00Z",
              "total": 94.0,
              "tax_lines": [],
              "fee_lines": [{ "name": "Booking fee", "amount": 4.0 }],
              "metadata": {
                "legs": [
                  { "origin": "London St Pancras", "destination": "Lille Europe", "departure": "2026-03-13T09:00:00Z", "arrival": "2026-03-13T10:30:00Z", "train_number": "EST9108", "class": "standard" },
                  { "origin": "Lille Europe", "destination": "Paris Nord", "departure": "2026-03-13T11:00:00Z", "arrival": "2026-03-13T12:05:00Z", "train_number": "EST9122", "class": "standard" }
                ]
              }
            }
            """;
        var railResult = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(railJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata.legs", hasSize(2)))
                .andReturn();
        String railId = objectMapper.readTree(railResult.getResponse().getContentAsString()).get("id").asText();

        // Trip Fee
        String tripFeeJson = """
            {
              "type": "navan_fee",
              "external_id": "navan-trip-fee-2026-03-11",
              "currency": "EUR",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 25.0,
              "tax_lines": [],
              "fee_lines": [],
              "metadata": { "fee_kind": "trip_fee", "description": "Navan trip fee", "related_external_id": "flight-TXL-LHR-2026-03-11" }
            }
            """;
        var tripFeeResult = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tripFeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata.fee_kind", is("trip_fee")))
                .andReturn();
        String tripFeeId = objectMapper.readTree(tripFeeResult.getResponse().getContentAsString()).get("id").asText();

        // Agent Call Fee
        String agentFeeJson = """
            {
              "type": "navan_fee",
              "external_id": "navan-agent-call-2026-03-11",
              "currency": "EUR",
              "occurred_at": "2026-03-11T18:00:00Z",
              "total": 15.0,
              "tax_lines": [],
              "fee_lines": [],
              "metadata": { "fee_kind": "agent_call_fee", "description": "Agent fee for support call", "related_external_id": "hotel-LON-2026-03-11" }
            }
            """;
        var agentFeeResult = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(agentFeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata.fee_kind", is("agent_call_fee")))
                .andReturn();
        String agentFeeId = objectMapper.readTree(agentFeeResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Duplicate external_id returns 409 conflict
        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(flightJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.id", is(flightId)));

        // 3. Create invoice with all 5 transaction IDs (grand total 685.70 EUR)
        InvoiceRequest invReq = new InvoiceRequest();
        invReq.setTransactionIds(List.of(flightId, hotelId, railId, tripFeeId, agentFeeId));

        var invResult = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total", is(685.70)))
                .andReturn();
        String invoiceId = objectMapper.readTree(invResult.getResponse().getContentAsString()).get("id").asText();

        // 4. Snapshot test: update flight total, invoice still shows 220.00
        String updatedFlightJson = """
            {
              "type": "flight",
              "external_id": "flight-TXL-LHR-2026-03-11",
              "currency": "EUR",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 500.0,
              "tax_lines": [{ "name": "Airport tax", "amount": 15.0 }],
              "fee_lines": [{ "name": "YQ", "amount": 25.0 }],
              "metadata": { "origin": "TXL", "destination": "LHR", "fare": 450.0, "coupons": [] }
            }
            """;
        mockMvc.perform(patch("/transactions/" + flightId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedFlightJson))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/invoices/" + invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.transaction_id == '" + flightId + "')].total", contains(220.0)));

        // 5. Mixed currency invoice returns 409
        String usdFlightJson = """
            {
              "type": "flight",
              "external_id": "flight-USD-123",
              "currency": "USD",
              "occurred_at": "2026-03-11T06:40:00Z",
              "total": 200.0,
              "tax_lines": [],
              "fee_lines": [],
              "metadata": { "origin": "JFK", "destination": "LHR", "fare": 180.0, "coupons": [] }
            }
            """;
        var usdRes = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(usdFlightJson))
                .andExpect(status().isCreated())
                .andReturn();
        String usdId = objectMapper.readTree(usdRes.getResponse().getContentAsString()).get("id").asText();

        InvoiceRequest mixedReq = new InvoiceRequest();
        mixedReq.setTransactionIds(List.of(flightId, usdId));
        mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mixedReq)))
                .andExpect(status().isConflict());

        // 6. Concurrent duplicate create test
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> {
            try {
                mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "type": "flight",
                              "external_id": "concurrent-ext-id",
                              "currency": "EUR",
                              "occurred_at": "2026-03-11T06:40:00Z",
                              "total": 100.0,
                              "tax_lines": [],
                              "fee_lines": [],
                              "metadata": { "origin": "A", "destination": "B", "fare": 90.0, "coupons": [] }
                            }
                            """));
            } catch (Exception ignored) {}
        });
        Future<?> f2 = executor.submit(() -> {
            try {
                mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "type": "flight",
                              "external_id": "concurrent-ext-id",
                              "currency": "EUR",
                              "occurred_at": "2026-03-11T06:40:00Z",
                              "total": 100.0,
                              "tax_lines": [],
                              "fee_lines": [],
                              "metadata": { "origin": "A", "destination": "B", "fare": 90.0, "coupons": [] }
                            }
                            """));
            } catch (Exception ignored) {}
        });
        f1.get();
        f2.get();
        executor.shutdown();

        // 7. DELETE transaction on invoice returns 409
        mockMvc.perform(delete("/transactions/" + flightId))
                .andExpect(status().isConflict());

        // 8. DELETE invoice succeeds, and underlying transaction can now be deleted
        mockMvc.perform(delete("/invoices/" + invoiceId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/transactions/" + flightId))
                .andExpect(status().isNoContent());

        // 9. Pagination limit exceeding 100 returns 400
        mockMvc.perform(get("/transactions?limit=1000"))
                .andExpect(status().isBadRequest());
    }
}
