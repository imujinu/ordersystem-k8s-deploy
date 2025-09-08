package com.order.ordersystem.product.controller;

import com.order.ordersystem.common.dto.CommonDto;
import com.order.ordersystem.product.domain.Product;
import com.order.ordersystem.product.dto.*;
import com.order.ordersystem.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("/create")
    ResponseEntity<?> create(@ModelAttribute ProductCreateDto productCreateDto, @RequestHeader("X-User-Email")String email){
        Long productId = productService.create(productCreateDto, email);
        return new ResponseEntity<>(CommonDto.builder()
                .result(productId)
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("상품등록완료")
                .build(), HttpStatus.CREATED);
    }

    @GetMapping("/list")
    ResponseEntity<?> list(@PageableDefault(value = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, ProductSearchDto productSearchDto){
        Page<ProductResDto> list = productService.findAll(pageable, productSearchDto);
        return new ResponseEntity<>(CommonDto.builder()
                .result(list)
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("상품조회완료")
                .build(), HttpStatus.CREATED);

    }

    @GetMapping("/detail/{id}")
    ResponseEntity<?> detail(@PathVariable Long id) throws InterruptedException {
        Thread.sleep(3000L);
        ProductResDto productResDto = productService.detail(id);
        return new ResponseEntity<>(CommonDto.builder()
                .result(productResDto)
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("상품조회완료")
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    ResponseEntity<?> update(@PathVariable Long id, ProductUpdateDto productUpdateDto){
         Product product = productService.update(id, productUpdateDto);
        return new ResponseEntity<>(CommonDto.builder()
                .result(product.getId())
                .statusCode(HttpStatus.ACCEPTED.value())
                .statusMessage("상품수정완료")
                .build(), HttpStatus.ACCEPTED);


    }

    @PutMapping("/updatestock")
    ResponseEntity<?> updatestock(@RequestBody ProductUpdateStockDto dto){
        System.out.println("재고 변경 시작");
         Long productId = productService.updateStock(dto);
        return new ResponseEntity<>(CommonDto.builder()
                .result(productId)
                .statusCode(HttpStatus.ACCEPTED.value())
                .statusMessage("상품 재고수량 변경완료")
                .build(), HttpStatus.ACCEPTED);


    }
}
