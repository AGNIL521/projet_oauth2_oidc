package ma.enset.orderservice.controller;

import lombok.extern.slf4j.Slf4j;
import ma.enset.orderservice.clients.ProductRestClient;
import ma.enset.orderservice.entities.Order;
import ma.enset.orderservice.entities.OrderLine;
import ma.enset.orderservice.model.Product;
import ma.enset.orderservice.repository.OrderRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRestClient productRestClient;

    public OrderController(OrderRepository orderRepository, ProductRestClient productRestClient) {
        this.orderRepository = orderRepository;
        this.productRestClient = productRestClient;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_USER') or hasAuthority('SCOPE_ADMIN')")
    public List<Order> getAllOrders() {
        log.info("User: {} requested all orders", getCurrentUser());
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_USER') or hasAuthority('SCOPE_ADMIN')")
    public Order getOrder(@PathVariable @NonNull Long id) {
        log.info("User: {} requested order with ID: {}", getCurrentUser(), id);
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_USER')")
    public Order createOrder(@RequestBody @NonNull Order order) {
        log.info("User: {} creating new order", getCurrentUser());
        order.setDate(LocalDate.now());
        order.setStatus("PENDING");
        double total = 0;
        
        // Validate products and calculate total
        if (order.getOrderLines() != null) {
            for (OrderLine line : order.getOrderLines()) {
                Product product = productRestClient.findProductById(line.getProductId());
                if (product == null) {
                    throw new RuntimeException("Product not found: " + line.getProductId());
                }
                
                // Check stock availability (Improvement based on evaluation)
                if (product.getQuantity() < line.getQuantity()) {
                     log.error("Insufficient stock for product: {}", product.getName());
                     throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                
                // Update price from product service
                line.setPrice(product.getPrice());
                line.setOrder(order);
                total += line.getPrice() * line.getQuantity();
            }
        }
        
        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} for User: {}", savedOrder.getId(), getCurrentUser());
        return savedOrder;
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
