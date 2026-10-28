package com.navan.takehome.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.takehome.dto.InvoiceRequest;
import com.navan.takehome.exception.BadRequestException;
import com.navan.takehome.exception.ConflictException;
import com.navan.takehome.exception.NotFoundException;
import com.navan.takehome.model.*;
import com.navan.takehome.repository.InvoiceRepository;
import com.navan.takehome.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          TransactionRepository transactionRepository,
                          ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createInvoice(InvoiceRequest req) {
        List<String> txnIds = resolveTransactionIds(req);
        if (txnIds.isEmpty()) {
            throw new BadRequestException("At least one transaction ID is required");
        }

        long uniqueCount = txnIds.stream().distinct().count();
        if (uniqueCount < txnIds.size()) {
            throw new ConflictException("Duplicate transaction IDs provided on invoice");
        }

        List<TransactionEntity> transactions = new ArrayList<>();
        for (String id : txnIds) {
            TransactionEntity txn = transactionRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
            transactions.add(txn);
        }

        String currency = transactions.get(0).getCurrency();
        for (var txn : transactions) {
            if (!txn.getCurrency().equals(currency)) {
                throw new ConflictException("Mixed currencies on invoice are not allowed");
            }
        }

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId("inv_" + UUID.randomUUID().toString().substring(0, 8));
        invoice.setCurrency(currency);

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (var txn : transactions) {
            grandTotal = grandTotal.add(txn.getTotal());

            InvoiceItemEntity item = new InvoiceItemEntity();
            item.setInvoice(invoice);
            item.setSourceTransactionId(txn.getId());
            item.setType(txn.getType());
            item.setExternalId(txn.getExternalId());
            item.setCurrency(txn.getCurrency());
            item.setOccurredAt(txn.getOccurredAt());
            item.setTotal(txn.getTotal());
            item.setTaxLinesJson(serializeTaxLines(txn.getTaxLines()));
            item.setFeeLinesJson(serializeFeeLines(txn.getFeeLines()));
            item.setMetadataJson(txn.getMetadataJson());
            invoice.getItems().add(item);

            InvoiceTransactionEntity invTxn = new InvoiceTransactionEntity();
            invTxn.setInvoice(invoice);
            invTxn.setTransactionId(txn.getId());
            invoice.getInvoiceTransactions().add(invTxn);
        }

        invoice.setTotal(grandTotal);
        invoice = invoiceRepository.save(invoice);
        return mapToResponse(invoice);
    }

    public Map<String, Object> getInvoice(String id) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
        return mapToResponse(invoice);
    }

    @Transactional
    public Map<String, Object> updateInvoice(String id, InvoiceRequest req) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));

        List<String> txnIds = resolveTransactionIds(req);
        if (txnIds.isEmpty()) {
            throw new BadRequestException("At least one transaction ID is required");
        }

        long uniqueCount = txnIds.stream().distinct().count();
        if (uniqueCount < txnIds.size()) {
            throw new ConflictException("Duplicate transaction IDs provided on invoice update");
        }

        List<TransactionEntity> transactions = new ArrayList<>();
        for (String txnId : txnIds) {
            TransactionEntity txn = transactionRepository.findById(txnId)
                    .orElseThrow(() -> new NotFoundException("Transaction not found: " + txnId));
            transactions.add(txn);
        }

        String currency = transactions.get(0).getCurrency();
        for (var txn : transactions) {
            if (!txn.getCurrency().equals(currency)) {
                throw new ConflictException("Mixed currencies on invoice are not allowed");
            }
        }

        invoice.getItems().clear();
        invoice.getInvoiceTransactions().clear();
        invoice.setCurrency(currency);

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (var txn : transactions) {
            grandTotal = grandTotal.add(txn.getTotal());

            InvoiceItemEntity item = new InvoiceItemEntity();
            item.setInvoice(invoice);
            item.setSourceTransactionId(txn.getId());
            item.setType(txn.getType());
            item.setExternalId(txn.getExternalId());
            item.setCurrency(txn.getCurrency());
            item.setOccurredAt(txn.getOccurredAt());
            item.setTotal(txn.getTotal());
            item.setTaxLinesJson(serializeTaxLines(txn.getTaxLines()));
            item.setFeeLinesJson(serializeFeeLines(txn.getFeeLines()));
            item.setMetadataJson(txn.getMetadataJson());
            invoice.getItems().add(item);

            InvoiceTransactionEntity invTxn = new InvoiceTransactionEntity();
            invTxn.setInvoice(invoice);
            invTxn.setTransactionId(txn.getId());
            invoice.getInvoiceTransactions().add(invTxn);
        }

        invoice.setTotal(grandTotal);
        invoiceRepository.save(invoice);
        return mapToResponse(invoice);
    }

    @Transactional
    public void deleteInvoice(String id) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
        invoiceRepository.delete(invoice);
    }

    public List<Map<String, Object>> listInvoices(int limit, int offset) {
        if (limit > 100) {
            throw new BadRequestException("Limit cannot exceed 100");
        }
        int page = offset / limit;
        var pageable = PageRequest.of(page, limit);
        return invoiceRepository.findAll(pageable).getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<String> resolveTransactionIds(InvoiceRequest req) {
        if (req.getTransactionIds() != null && !req.getTransactionIds().isEmpty()) {
            return req.getTransactionIds();
        } else if (req.getTransactionId() != null && !req.getTransactionId().isBlank()) {
            return List.of(req.getTransactionId());
        }
        return List.of();
    }

    private String serializeTaxLines(List<TaxLineEntity> lines) {
        try {
            List<Map<String, Object>> list = lines.stream().map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", t.getName());
                if (t.getRate() != null) m.put("rate", t.getRate());
                m.put("amount", t.getAmount());
                return m;
            }).collect(Collectors.toList());
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String serializeFeeLines(List<FeeLineEntity> lines) {
        try {
            List<Map<String, Object>> list = lines.stream().map(f -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", f.getName());
                m.put("amount", f.getAmount());
                return m;
            }).collect(Collectors.toList());
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public Map<String, Object> mapToResponse(InvoiceEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("currency", entity.getCurrency());
        map.put("total", entity.getTotal());
        map.put("created_at", entity.getCreatedAt());

        List<Map<String, Object>> itemsList = new ArrayList<>();
        Map<String, BigDecimal> taxRollupMap = new LinkedHashMap<>();
        Map<String, BigDecimal> feeRollupMap = new LinkedHashMap<>();

        for (var item : entity.getItems()) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("transaction_id", item.getSourceTransactionId());
            itemMap.put("type", item.getType());
            itemMap.put("external_id", item.getExternalId());
            itemMap.put("currency", item.getCurrency());
            itemMap.put("occurred_at", item.getOccurredAt());
            itemMap.put("total", item.getTotal());
            try {
                List<Map<String, Object>> taxes = objectMapper.readValue(item.getTaxLinesJson(), List.class);
                itemMap.put("tax_lines", taxes);
                for (var t : taxes) {
                    String name = (String) t.get("name");
                    BigDecimal amt = new BigDecimal(t.get("amount").toString());
                    taxRollupMap.put(name, taxRollupMap.getOrDefault(name, BigDecimal.ZERO).add(amt));
                }

                List<Map<String, Object>> fees = objectMapper.readValue(item.getFeeLinesJson(), List.class);
                itemMap.put("fee_lines", fees);
                for (var f : fees) {
                    String name = (String) f.get("name");
                    BigDecimal amt = new BigDecimal(f.get("amount").toString());
                    feeRollupMap.put(name, feeRollupMap.getOrDefault(name, BigDecimal.ZERO).add(amt));
                }

                itemMap.put("metadata", objectMapper.readValue(item.getMetadataJson(), Map.class));
            } catch (Exception e) {
                itemMap.put("tax_lines", List.of());
                itemMap.put("fee_lines", List.of());
                itemMap.put("metadata", Map.of());
            }
            itemsList.add(itemMap);
        }

        map.put("items", itemsList);

        map.put("tax_rollup", taxRollupMap.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("amount", e.getValue());
            return m;
        }).collect(Collectors.toList()));

        map.put("fee_rollup", feeRollupMap.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("amount", e.getValue());
            return m;
        }).collect(Collectors.toList()));

        return map;
    }
}
