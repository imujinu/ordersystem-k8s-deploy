package com.order.ordersystem.product.dto;

import com.order.ordersystem.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateDto {
    private String name;
    private String category;
    private int price;
    private int stockQuantity;
//    private MultipartFile productImage;

    public Product toEntity(String email){
        return Product.builder()
                .name(this.name)
                .category(this.category)
                .price(this.price)
                .stockQuantity(this.stockQuantity)
                .memberEmail(email)
//                .imagePath(imgUrl)
                .build();
    }
}
