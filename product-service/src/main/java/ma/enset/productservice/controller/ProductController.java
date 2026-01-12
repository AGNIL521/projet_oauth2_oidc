package ma.enset.productservice.controller;

import lombok.extern.slf4j.Slf4j;
import ma.enset.productservice.entities.Product;
import ma.enset.productservice.repository.ProductRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@Slf4j
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public List<Product> getAllProducts() {
        log.info("User: {} requested all products", getCurrentUser());
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public Product getProduct(@PathVariable @NonNull Long id) {
        log.info("User: {} requested product with ID: {}", getCurrentUser(), id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public Product createProduct(@RequestBody @NonNull Product product) {
        log.info("User: {} creating product: {}", getCurrentUser(), product);
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Product updateProduct(@PathVariable @NonNull Long id, @RequestBody @NonNull Product product) {
        log.info("User: {} updating product with ID: {}", getCurrentUser(), id);
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setQuantity(product.getQuantity());
        return productRepository.save(existingProduct);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteProduct(@PathVariable @NonNull Long id) {
        log.info("User: {} deleting product with ID: {}", getCurrentUser(), id);
        productRepository.deleteById(id);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
