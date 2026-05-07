package com.sneaky.sneaky.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class CoreEntityMappingTest {

    @Test
    void brandNameIsUniqueAndRequired() throws Exception {
        Table table = Brands.class.getAnnotation(Table.class);
        Column nameColumn = Brands.class.getDeclaredField("name").getAnnotation(Column.class);

        assertThat(table.name()).isEqualTo("brands");
        assertThat(table.uniqueConstraints()).singleElement().satisfies(uniqueConstraint ->
                assertThat(uniqueConstraint.columnNames()).containsExactly("name"));
        assertThat(nameColumn.nullable()).isFalse();
        assertThat(nameColumn.unique()).isTrue();
    }

    @Test
    void productUsesUuidIdAndLazyOptionalBrand() throws Exception {
        GeneratedValue idGenerator = Products.class.getDeclaredField("productId").getAnnotation(GeneratedValue.class);
        ManyToOne brandRelation = Products.class.getDeclaredField("brand").getAnnotation(ManyToOne.class);
        JoinColumn brandJoin = Products.class.getDeclaredField("brand").getAnnotation(JoinColumn.class);
        Column priceColumn = Products.class.getDeclaredField("price").getAnnotation(Column.class);

        assertThat(idGenerator.strategy()).isEqualTo(GenerationType.UUID);
        assertThat(brandRelation.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(brandJoin.name()).isEqualTo("brand_id");
        assertThat(priceColumn.precision()).isEqualTo(10);
        assertThat(priceColumn.scale()).isEqualTo(2);
    }

    @Test
    void userEmailAndPasswordAreRequiredAndEmailIsUnique() throws Exception {
        GeneratedValue idGenerator = Users.class.getDeclaredField("userId").getAnnotation(GeneratedValue.class);
        Column emailColumn = Users.class.getDeclaredField("email").getAnnotation(Column.class);
        Column passwordColumn = Users.class.getDeclaredField("password").getAnnotation(Column.class);

        assertThat(idGenerator.strategy()).isEqualTo(GenerationType.UUID);
        assertThat(emailColumn.name()).isEqualTo("email");
        assertThat(emailColumn.unique()).isTrue();
        assertThat(emailColumn.nullable()).isFalse();
        assertThat(passwordColumn.nullable()).isFalse();
        assertThat(new Users().getIsGuest()).isFalse();
    }

    @Test
    void swipeStoresActionAsStringWithRequiredLazyRelations() throws Exception {
        ManyToOne userRelation = Swipe.class.getDeclaredField("user").getAnnotation(ManyToOne.class);
        JoinColumn userJoin = Swipe.class.getDeclaredField("user").getAnnotation(JoinColumn.class);
        ManyToOne productRelation = Swipe.class.getDeclaredField("product").getAnnotation(ManyToOne.class);
        JoinColumn productJoin = Swipe.class.getDeclaredField("product").getAnnotation(JoinColumn.class);

        assertThat(userRelation.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(userJoin.name()).isEqualTo("user_id");
        assertThat(userJoin.nullable()).isFalse();
        assertThat(productRelation.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(productJoin.name()).isEqualTo("product_id");
        assertThat(productJoin.nullable()).isFalse();
        assertThat(SwipeAction.valueOf("LIKE")).isEqualTo(SwipeAction.LIKE);
    }

    @Test
    void buildersAndAccessorsKeepEntityState() {
        UUID brandId = UUID.randomUUID();
        Brands brand = Brands.builder().id(brandId).name("Nike").build();
        Products product = Products.builder()
                .productId(UUID.randomUUID())
                .brand(brand)
                .name("Air Max")
                .build();

        assertThat(brand.getId()).isEqualTo(brandId);
        assertThat(brand.getName()).isEqualTo("Nike");
        assertThat(product.getBrand()).isEqualTo(brand);
        assertThat(product.getName()).isEqualTo("Air Max");
    }
}
