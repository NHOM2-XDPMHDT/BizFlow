package com.example.bizflow.service;

import com.example.bizflow.entity.Product;
import com.example.bizflow.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        long count = productRepository.count();
        if (count > 0) {
            logger.info("Products already exist ({}). Checking for inactive status to fix.", count);
            // Fixup: if many products have null or inactive status, set them to active so UI shows them
            List<Product> all = productRepository.findAll();
            List<Product> toEnable = new ArrayList<>();
            for (Product p : all) {
                if (p.getStatus() == null || "inactive".equalsIgnoreCase(p.getStatus())) {
                    p.setStatus("active");
                    toEnable.add(p);
                }
            }
            if (!toEnable.isEmpty()) {
                productRepository.saveAll(toEnable);
                logger.info("Updated {} products to active=true.", toEnable.size());
            } else {
                logger.info("No products needed active=true fixup.");
            }
            return;
        }

        logger.info("No products found. Seeding sample products from classpath:data/products.csv");
        ClassPathResource resource = new ClassPathResource("data/products.csv");
        List<Product> products = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                String[] cols = line.split(",");
                if (cols.length < 3) continue;
                String code = cols[0].trim();
                String name = cols[1].trim();
                BigDecimal price = new BigDecimal(cols[2].trim());
                Product p = new Product(code, name, price);
                // optional category id if present
                if (cols.length >= 4 && !cols[3].trim().isEmpty()) {
                    try { p.setCategoryId(Long.valueOf(cols[3].trim())); } catch (NumberFormatException ignored) {}
                }
                products.add(p);
            }
        }

        if (!products.isEmpty()) {
            productRepository.saveAll(products);
            logger.info("Seeded {} products.", products.size());
        } else {
            logger.warn("No products found in resource data/products.csv to seed.");
        }
    }
}
