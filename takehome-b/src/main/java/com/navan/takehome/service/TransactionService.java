package com.navan.takehome.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.takehome.dto.*;
import com.navan.takehome.exception.BadRequestException;
import com.navan.takehome.exception.ConflictException;
import com.navan.takehome.exception.NotFoundException;
import com.navan.takehome.model.*;
import com.navan.takehome.repository.InvoiceTransactionRepository;
import com.navan.takehome.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final InvoiceTransactionRepository invoiceTransactionRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              InvoiceTransactionRepository invoiceTransactionRepository,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.invoiceTransactionRepository = invoiceTransactionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createTransaction(TransactionRequest req) {
        validateMetadata(req.getType(), req.getMetadata());

        var existing = transactionRepository.findByExternalId(req.getExternalId());
        if (existing.isPresent()) {
            throw new ConflictException("Transaction with external_id already exists", mapToResponse(existing.get()));
        }

        TransactionEntity entity = new TransactionEntity();
        entity.setId("txn_" + UUID.randomUUID().toString().substring(0, 8));
        entity.setType(req.getType());
        entity.setExternalId(req.getExternalId());
        entity.setCurrency(req.getCurrency());
        entity.setOccurredAt(req.getOccurredAt());
        entity.setTotal(req.getTotal());
        try {
            entity.setMetadataJson(objectMapper.writeValueAsString(req.getMetadata()));
        } catch (Exception e) {
            throw new BadRequestException("Invalid metadata format");
        }

        if (req.getTaxLines() != null) {
            for (var t : req.getTaxLines()) {
                TaxLineEntity tax = new TaxLineEntity();
                tax.setTransaction(entity);
                tax.setName(t.getName());
                tax.setRate(t.getRate());
                tax.setAmount(t.getAmount());
                entity.getTaxLines().add(tax);
            }
        }

        if (req.getFeeLines() != null) {
            for (var f : req.getFeeLines()) {
                FeeLineEntity fee = new FeeLineEntity();
                fee.setTransaction(entity);
                fee.setName(f.getName());
                fee.setAmount(f.getAmount());
                entity.getFeeLines().add(fee);
            }
        }

        try {
            entity = transactionRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            var ex = transactionRepository.findByExternalId(req.getExternalId()).orElseThrow();
            throw new ConflictException("Duplicate external_id under concurrency", mapToResponse(ex));
        }

        return mapToResponse(entity);
    }

    public Map<String, Object> getTransaction(String id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        return mapToResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateTransaction(String id, TransactionRequest req) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (invoiceTransactionRepository.existsByTransactionId(id)) {
            throw new ConflictException("Cannot update transaction already included in an invoice");
        }

        validateMetadata(req.getType(), req.getMetadata());
        entity.setType(req.getType());
        entity.setCurrency(req.getCurrency());
        entity.setOccurredAt(req.getOccurredAt());
        entity.setTotal(req.getTotal());
        entity.setUpdatedAt(Instant.now());
        try {
            entity.setMetadataJson(objectMapper.writeValueAsString(req.getMetadata()));
        } catch (Exception e) {
            throw new BadRequestException("Invalid metadata format");
        }

        entity.getTaxLines().clear();
        if (req.getTaxLines() != null) {
            for (var t : req.getTaxLines()) {
                TaxLineEntity tax = new TaxLineEntity();
                tax.setTransaction(entity);
                tax.setName(t.getName());
                tax.setRate(t.getRate());
                tax.setAmount(t.getAmount());
                entity.getTaxLines().add(tax);
            }
        }

        entity.getFeeLines().clear();
        if (req.getFeeLines() != null) {
            for (var f : req.getFeeLines()) {
                FeeLineEntity fee = new FeeLineEntity();
                fee.setTransaction(entity);
                fee.setName(f.getName());
                fee.setAmount(f.getAmount());
                entity.getFeeLines().add(fee);
            }
        }

        transactionRepository.save(entity);
        return mapToResponse(entity);
    }

    @Transactional
    public void deleteTransaction(String id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));

        if (invoiceTransactionRepository.existsByTransactionId(id)) {
            throw new ConflictException("Cannot delete transaction already included in an invoice");
        }

        transactionRepository.delete(entity);
    }

    public List<Map<String, Object>> listTransactions(int limit, int offset) {
        if (limit > 100) {
            throw new BadRequestException("Limit cannot exceed 100");
        }
        int page = offset / limit;
        var pageable = PageRequest.of(page, limit);
        return transactionRepository.findAll(pageable).getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateMetadata(String type, Map<String, Object> metadata) {
        if (metadata == null) throw new BadRequestException("Metadata is required");
        switch (type) {
            case "flight":
                if (!metadata.containsKey("origin") || !metadata.containsKey("destination") ||
                    !metadata.containsKey("fare") || !metadata.containsKey("coupons")) {
                    throw new BadRequestException("Flight metadata requires origin, destination, fare, and coupons");
                }
                break;
            case "hotel":
                if (!metadata.containsKey("name") || !metadata.containsKey("address") ||
                    !metadata.containsKey("check_in") || !metadata.containsKey("check_out") ||
                    !metadata.containsKey("nights") || !metadata.containsKey("room_rate_per_night")) {
                    throw new BadRequestException("Hotel metadata requires name, address, check_in, check_out, nights, room_rate_per_night");
                }
                break;
            case "rail":
                if (!metadata.containsKey("legs")) {
                    throw new BadRequestException("Rail metadata requires legs");
                }
                break;
            case "navan_fee":
                if (!metadata.containsKey("fee_kind")) {
                    throw new BadRequestException("Navan fee metadata requires fee_kind");
                }
                String kind = (String) metadata.get("fee_kind");
                if (!kind.equals("trip_fee") && !kind.equals("agent_call_fee")) {
                    throw new BadRequestException("Unknown navan_fee.fee_kind: " + kind);
                }
                break;
            default:
                throw new BadRequestException("Unknown transaction type: " + type);
        }
    }

    public Map<String, Object> mapToResponse(TransactionEntity entity) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("type", entity.getType());
        map.put("external_id", entity.getExternalId());
        map.put("currency", entity.getCurrency());
        map.put("occurred_at", entity.getOccurredAt());
        map.put("total", entity.getTotal());
        map.put("tax_lines", entity.getTaxLines().stream().map(t -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("name", t.getName());
            if (t.getRate() != null) m.put("rate", t.getRate());
            m.put("amount", t.getAmount());
            return m;
        }).collect(Collectors.toList()));
        map.put("fee_lines", entity.getFeeLines().stream().map(f -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("name", f.getName());
            m.put("amount", f.getAmount());
            return m;
        }).collect(Collectors.toList()));
        try {
            map.put("metadata", objectMapper.readValue(entity.getMetadataJson(), Map.class));
        } catch (Exception e) {
            map.put("metadata", Map.of());
        }
        return map;
    }
}
