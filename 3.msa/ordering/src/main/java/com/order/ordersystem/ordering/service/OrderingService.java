package com.order.ordersystem.ordering.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.ordersystem.common.config.KafkaProducerConfig;
import com.order.ordersystem.common.dto.CommonDto;
import com.order.ordersystem.common.service.SseAlarmService;
import com.order.ordersystem.ordering.domain.Ordering;
import com.order.ordersystem.ordering.domain.OrderingDetail;
import com.order.ordersystem.ordering.dto.OrderCreateDto;
import com.order.ordersystem.ordering.dto.OrderDetail;
import com.order.ordersystem.ordering.dto.OrderListResDto;
import com.order.ordersystem.ordering.dto.ProductDto;
import com.order.ordersystem.ordering.feignclient.ProductFeignClient;
import com.order.ordersystem.ordering.repository.OrderDetailRepository;
import com.order.ordersystem.ordering.repository.OrderingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final SseAlarmService sseAlarmService;
    private final RestTemplate restTemplate;
    private final ProductFeignClient productFeignClient;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    public Long create(List<OrderCreateDto> orderCreateDto, String email) {
        Ordering ordering = orderingRepository.save(Ordering.builder()
                        .memberEmail(email)
                        .build());

        List<OrderingDetail> orderingDetails = new ArrayList<>();
        orderCreateDto.forEach(a -> {

            // 상품 조회
            String productDetailUrl = "http://product-service/product/detail/" + a.getProductId();
            HttpHeaders headers = new HttpHeaders();
            //httpEntity : httpBody와 httpHeader를 셋팅하기 위한 객체
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<CommonDto> responseEntity = restTemplate.exchange(productDetailUrl, HttpMethod.GET,httpEntity, CommonDto.class);
            CommonDto commonDto = responseEntity.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            //readValue : String -> Class 변환, covertValue : Object -> 내가원하는 클래스로 변환
            ProductDto product = objectMapper.convertValue(commonDto.getResult(), ProductDto.class);
            if (a.getProductCount() > product.getStockQuantity()) {
               throw new IllegalArgumentException("재고 부족");
            }else{
              //주문 발생
                orderingDetails.add(OrderingDetail.builder()
                        .productId(product.getId())
                        .productName(product.getProductName())
                        .quantity(a.getProductCount())
                        .ordering(ordering)
                        .build());
            // 동기적 재고감소 요청
                String productUpdateStockUrl = "http://product-service/product/updatestock";
                HttpHeaders stockHeaders = new HttpHeaders();
                stockHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<OrderCreateDto> updateStockEntity = new HttpEntity<>(a, stockHeaders);
                restTemplate.exchange(productUpdateStockUrl, HttpMethod.PUT, updateStockEntity, void.class);
                System.out.println("디버깅2");
            }
            ordering.getOrderingDetails().addAll(orderingDetails);
        });


        orderingRepository.save(ordering);
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());

            return ordering.getId();
    }

    //fallback 메서드는 원본 메서드의 매개변수와 정확하게 일치해야함.
    public void fallbackProductServiceCircuit(List<OrderCreateDto> orderCreateDto, String email, Throwable t){
        throw new RuntimeException("상품 서버 응답 없음. 나중에 다시 시도해 주세요");
    }

    // 테스트 : 4~5번의 정상 요청 -> 5번 중에 2번의 지연 발생 -> circuit open -> 그 다음 요청은 바로 fallback

    @CircuitBreaker(name = "productServiceCircuit", fallbackMethod = "fallbackProductServiceCircuit")
    public Long createFeignKafka(List<OrderCreateDto> orderCreateDto, String email) {

        Ordering ordering = orderingRepository.save(Ordering.builder()
                        .memberEmail(email)
                        .build());

        List<OrderingDetail> orderingDetails = new ArrayList<>();
        orderCreateDto.forEach(a -> {
            // feign 클라이언트를 사용한 동기적 상품 조회
            CommonDto commonDto = productFeignClient.getProductById(a.getProductId());
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDto product = objectMapper.convertValue(commonDto.getResult(), ProductDto.class);


            if (a.getProductCount() > product.getStockQuantity()) {
               throw new IllegalArgumentException("재고 부족");
            }else{
              //주문 발생
                System.out.println("debug!!");
                orderingDetails.add(OrderingDetail.builder()
                        .productId(product.getId())
                        .productName(product.getProductName())
                        .quantity(a.getProductCount())
                        .ordering(ordering)
                        .build());


                System.out.println("디버깅1`");

                //feign을 통한 동기적 재고감소 요청
//                productFeignClient.updateProductStockQuantity(a);

                // kafka를 이용한 비동기적 재고감소 요청
                kafkaTemplate.send("stock-update-topic", a);

            ordering.getOrderingDetails().addAll(orderingDetails);
            }
        });


        orderingRepository.save(ordering);
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());

            return ordering.getId();

    }

    public List<OrderListResDto> findAll() {
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos = getOrderListResDtos(orderings);
        return orderListResDtos;
    }


    public List<OrderListResDto> findByMember(String email) {
        List<Ordering> orderings = orderingRepository.findAllByMemberEmail(email);
        List<OrderListResDto> orderListResDtos = getOrderListResDtos(orderings);
        if(orderListResDtos.isEmpty()){
            throw new EntityNotFoundException("주문 정보가 없습니다.");
        }
        return orderListResDtos;
    }

    private List<OrderListResDto> getOrderListResDtos(List<Ordering> orderings) {
        List<OrderListResDto> orderListResDtos = new ArrayList<>();
        orderings.forEach(a -> {
            List<OrderingDetail> orderingDetails = a.getOrderingDetails();
            List<OrderDetail> orderDetails = orderingDetails.stream()
                    .map(ad -> new OrderDetail()
                    .fromEntity(ad)).collect(Collectors.toList());
            orderListResDtos.add(new OrderListResDto().fromEntity(a, orderDetails));
        });
        return orderListResDtos;
    }

    //격리 레벨을 낮춤으로서 성능 향상과, lock 관련 문제 원천 차단.

}
