package com.sneaky.sneaky.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.brand.BrandDTO;
import com.sneaky.sneaky.dto.brand.CreateBrandRequestDTO;
import com.sneaky.sneaky.dto.brand.UpdateBrandRequestDTO;
import com.sneaky.sneaky.dto.product.CreateProductRequestDTO;
import com.sneaky.sneaky.dto.product.ProductDTO;
import com.sneaky.sneaky.dto.product.UpdateProductRequestDTO;
import com.sneaky.sneaky.dto.user.UpdateUserRequestDTO;
import com.sneaky.sneaky.dto.user.UserDTO;
import com.sneaky.sneaky.entity.Products;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.BrandsRepository;
import com.sneaky.sneaky.repository.ProductsRepository;
import com.sneaky.sneaky.repository.SwipesRepository;
import com.sneaky.sneaky.repository.UsersRepository;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.BrandService;
import com.sneaky.sneaky.services.ProductService;
import com.sneaky.sneaky.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final List<String> ROLES = List.of("USER", "ADMIN", "MODERATOR");

    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;
    private final BrandsRepository brandsRepository;
    private final SwipesRepository swipesRepository;
    private final UserService userService;
    private final ProductService productService;
    private final BrandService brandService;
    private final CurrentUser currentUser;

    @GetMapping("/dashboard/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", usersRepository.count());
        stats.put("admins", usersRepository.countByRole("ADMIN"));
        stats.put("bannedUsers", usersRepository.countByIsBanned(true));
        stats.put("totalProducts", productsRepository.count());
        stats.put("pendingProducts", productsRepository.countByStatus("PENDING"));
        stats.put("approvedProducts", productsRepository.countByStatus("APPROVED"));
        stats.put("totalBrands", brandsRepository.count());
        stats.put("totalSwipes", swipesRepository.count());
        stats.put("todaySwipes", swipesRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));
        return stats;
    }

    @GetMapping("/users")
    public Page<Users> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean banned) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Users> users = role != null
                ? usersRepository.findByRole(role.toUpperCase(), pageable)
                : banned != null ? usersRepository.findByIsBanned(banned, pageable) : usersRepository.findAll(pageable);

        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @GetMapping("/users/{userId}")
    public Map<String, Object> user(@PathVariable UUID userId) {
        Users user = findUser(userId);
        user.setPassword(null);

        return Map.of(
                "user", user,
                "stats", Map.of(
                        "totalSwipes", swipesRepository.countByUserUserId(userId),
                        "memberSince", user.getCreatedAt()));
    }

    @PatchMapping("/users/{userId}")
    public UserDTO editUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequestDTO request) {
        return userService.patchUserById(userId, request);
    }

    @PutMapping("/users/{userId}/ban")
    public Map<String, Object> banUser(@PathVariable UUID userId, @RequestBody Map<String, Boolean> request) {
        Users user = findUser(userId);
        boolean banned = Boolean.TRUE.equals(request.get("banned"));

        user.setIsBanned(banned);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);

        return Map.of("userId", userId, "isBanned", banned);
    }

    @PutMapping("/users/{userId}/role")
    public Map<String, Object> changeRole(@PathVariable UUID userId, @RequestBody Map<String, String> request) {
        String role = request.getOrDefault("role", "").trim().toUpperCase();
        if (!ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        Users user = findUser(userId);
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.save(user);

        return Map.of("userId", userId, "role", role);
    }

    @GetMapping("/products")
    public Page<Products> products(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return status == null || status.isBlank()
                ? productsRepository.findAll(pageable)
                : productsRepository.findByStatus(status.toUpperCase(), pageable);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO createProduct(@RequestBody CreateProductRequestDTO request) {
        return productService.createProduct(request);
    }

    @PatchMapping("/products/{productId}")
    public ProductDTO editProduct(@PathVariable UUID productId, @RequestBody UpdateProductRequestDTO request) {
        return productService.patchProduct(productId, request);
    }

    @PutMapping("/products/{productId}/approve")
    public Map<String, String> approveProduct(@PathVariable UUID productId) {
        Products product = findProduct(productId);
        product.setStatus("APPROVED");
        product.setApprovedBy(currentUser.getUserId());
        product.setApprovedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productsRepository.save(product);
        return Map.of("message", "Product approved");
    }

    @PutMapping("/products/{productId}/reject")
    public Map<String, String> rejectProduct(@PathVariable UUID productId, @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "").trim();
        if (reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }

        Products product = findProduct(productId);
        product.setStatus("REJECTED");
        product.setIsActive(false);
        product.setRejectionReason(reason);
        product.setUpdatedAt(LocalDateTime.now());
        productsRepository.save(product);

        return Map.of("message", "Product rejected", "reason", reason);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable UUID productId) {
        Products product = findProduct(productId);
        product.setIsActive(false);
        product.setStatus("INACTIVE");
        product.setUpdatedAt(LocalDateTime.now());
        productsRepository.save(product);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandDTO createBrand(@Valid @RequestBody CreateBrandRequestDTO request) {
        return brandService.createBrand(request);
    }

    @PutMapping("/brands/{brandId}")
    public BrandDTO editBrand(@PathVariable UUID brandId, @Valid @RequestBody UpdateBrandRequestDTO request) {
        return brandService.updateBrand(brandId, request);
    }

    @DeleteMapping("/brands/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable UUID brandId) {
        brandService.deleteBrand(brandId);
    }

    private Users findUser(UUID userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Products findProduct(UUID productId) {
        return productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
