package com.order.ordersystem.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.ordersystem.product.dto.ProductUpdateStockDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockKafkaListener {
    private final ProductService productService;

    @KafkaListener(topics = "stock-update-topic" , containerFactory = "kafkaListener")
    public void stockConsumer(String message){
        System.out.println("consumer message : " + message);
        ObjectMapper objectMapper = new ObjectMapper();
        ProductUpdateStockDto dto = null;
        try {
            dto = objectMapper.readValue(message, ProductUpdateStockDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        productService.updateStock(dto);

    }
}
