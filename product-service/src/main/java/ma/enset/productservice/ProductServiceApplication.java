package ma.enset.productservice;

import ma.enset.productservice.entities.Product;
import ma.enset.productservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Objects;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(ProductRepository productRepository) {
        return args -> {
            productRepository.save(Objects.requireNonNull(Product.builder().name("Laptop").description("High performance laptop").price(1200.0).quantity(10).build()));
            productRepository.save(Objects.requireNonNull(Product.builder().name("Smartphone").description("Latest smartphone").price(800.0).quantity(20).build()));
            productRepository.save(Objects.requireNonNull(Product.builder().name("Headphones").description("Noise cancelling headphones").price(200.0).quantity(50).build()));
        };
    }
}
