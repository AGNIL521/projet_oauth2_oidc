package ma.enset.orderservice.clients;

import ma.enset.orderservice.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://product-service:8081")
public interface ProductRestClient {
    @GetMapping("/products/{id}")
    Product findProductById(@PathVariable Long id);
}
