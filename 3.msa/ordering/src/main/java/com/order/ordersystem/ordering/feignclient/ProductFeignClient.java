package com.order.ordersystem.ordering.feignclient;

import com.order.ordersystem.common.dto.CommonDto;
import com.order.ordersystem.ordering.dto.OrderCreateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

//name 부분은 eureka에 등록된 application.name을 의미한다.
// url 부분은 k8s의 service 명
@FeignClient(name= "product-service", url="http://product-service")
public interface ProductFeignClient {
    @GetMapping("/product/detail/{productId}")
    CommonDto getProductById(@PathVariable Long productId);

    @PutMapping("/product/updatestock")
    void updateProductStockQuantity(@RequestBody OrderCreateDto dto);
}
