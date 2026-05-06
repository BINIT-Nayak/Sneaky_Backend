package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.brand.BrandDTO;
import com.sneaky.sneaky.dto.brand.CreateBrandRequestDTO;
import com.sneaky.sneaky.dto.brand.UpdateBrandRequestDTO;
import com.sneaky.sneaky.entity.Brands;
import com.sneaky.sneaky.repository.BrandsRepository;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandsRepository brandsRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    void createBrandTrimsNameAndRejectsDuplicates() {
        CreateBrandRequestDTO request = new CreateBrandRequestDTO();
        request.setName(" Nike ");

        when(brandsRepository.existsByNameIgnoreCase("Nike")).thenReturn(false);
        when(brandsRepository.save(any(Brands.class))).thenAnswer(invocation -> {
            Brands brand = invocation.getArgument(0);
            brand.setId(UUID.randomUUID());
            return brand;
        });

        BrandDTO created = brandService.createBrand(request);

        ArgumentCaptor<Brands> captor = ArgumentCaptor.forClass(Brands.class);
        verify(brandsRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Nike");
        assertThat(created.getName()).isEqualTo("Nike");
    }

    @Test
    void createBrandRejectsDuplicateName() {
        CreateBrandRequestDTO request = new CreateBrandRequestDTO();
        request.setName("Nike");

        when(brandsRepository.existsByNameIgnoreCase("Nike")).thenReturn(true);

        assertThatThrownBy(() -> brandService.createBrand(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(brandsRepository, never()).save(any(Brands.class));
    }

    @Test
    void getAllBrandsMapsEntitiesToDtos() {
        UUID brandId = UUID.randomUUID();
        Brands brand = Brands.builder().id(brandId).name("Adidas").build();

        when(brandsRepository.findAll()).thenReturn(List.of(brand));

        assertThat(brandService.getAllBrands()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(brandId);
            assertThat(dto.getName()).isEqualTo("Adidas");
        });
    }

    @Test
    void updateBrandRejectsNameUsedByAnotherBrand() {
        UUID brandId = UUID.randomUUID();
        Brands brand = Brands.builder().id(brandId).name("Nike").build();
        UpdateBrandRequestDTO request = new UpdateBrandRequestDTO();
        request.setName("Adidas");

        when(brandsRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandsRepository.existsByNameIgnoreCase("Adidas")).thenReturn(true);

        assertThatThrownBy(() -> brandService.updateBrand(brandId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteBrandDeletesExistingBrand() {
        UUID brandId = UUID.randomUUID();
        Brands brand = Brands.builder().id(brandId).name("Puma").build();

        when(brandsRepository.findById(brandId)).thenReturn(Optional.of(brand));

        brandService.deleteBrand(brandId);

        verify(brandsRepository).delete(brand);
    }
}
