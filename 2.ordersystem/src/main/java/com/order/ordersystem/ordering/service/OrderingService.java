package com.order.ordersystem.ordering.service;

import com.order.ordersystem.common.service.SseAlarmService;
import com.order.ordersystem.member.domain.Member;
import com.order.ordersystem.member.repository.MemberRepository;
import com.order.ordersystem.ordering.domain.Ordering;
import com.order.ordersystem.ordering.domain.OrderingDetail;
import com.order.ordersystem.ordering.dto.OrderCreateDto;
import com.order.ordersystem.ordering.dto.OrderDetail;
import com.order.ordersystem.ordering.dto.OrderListResDto;
import com.order.ordersystem.ordering.repository.OrderDetailRepository;
import com.order.ordersystem.ordering.repository.OrderingRepository;
import com.order.ordersystem.product.domain.Product;
import com.order.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final SseAlarmService sseAlarmService;
    public Long create(List<OrderCreateDto> orderCreateDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("존재하지 않는 유저입니다."));
        Ordering ordering = orderingRepository.save(Ordering.builder()
                        .member(member)
                        .build());
        StringBuffer sb = new StringBuffer();

        List<OrderingDetail> orderingDetails = new ArrayList<>();

        orderCreateDto.forEach(a -> {
            Product product = productRepository.findById(a.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));

            if (a.getProductCount() > product.getStockQuantity()) {
               throw new IllegalArgumentException("재고 부족");
            }else{
                // 1. 동시에 접근하는 상황에서 update 값의 정합성이 깨지고 갱신이상이 발생
                // 2. spring 버전이나 mysql 버전에 따라 jpa에서 강제에러를 유발시켜 대부분의 요청 실패 발생
                product.minusStock(a.getProductCount());
                orderingDetails.add(OrderingDetail.builder()
                        .product(product)
                        .quantity(a.getProductCount())
                        .ordering(ordering)
                        .build());
            }
        });

        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());

            ordering.getOrderingDetails().addAll(orderingDetails);
            return ordering.getId();

    }


    public List<OrderListResDto> findAll() {
        List<Ordering> orderings = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos = getOrderListResDtos(orderings);
        return orderListResDtos;
    }


    public List<OrderListResDto> findByMember() {
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 유저입니다."));
        List<Ordering> orderings = orderingRepository.findAllByMember(member);
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

    public Ordering cancel(Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("존재하지 않는 유저입니다."));
        Ordering ordering = orderingRepository.findByIdAndMember(id,member).orElseThrow(()->new EntityNotFoundException("주문이 존재하지 않습니다."));
        ordering.cancel();
        ordering.getOrderingDetails().stream().forEach(a->{
        });
        return ordering;
    }
}
