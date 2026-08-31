package com.example.reservas.web;

import com.example.reservas.domain.Business;
import com.example.reservas.domain.Reservation;
import com.example.reservas.domain.Resource;
import com.example.reservas.domain.User;
import com.example.reservas.repo.BusinessRepository;
import com.example.reservas.repo.ReservationRepository;
import com.example.reservas.repo.ResourceRepository;
import com.example.reservas.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusinessControllerDeleteTest {

    @Mock
    private BusinessRepository businessRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private ResourceRepository resourceRepo;

    @Mock
    private ReservationRepository reservationRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BusinessController(businessRepo, userRepo, resourceRepo, reservationRepo)).build();
    }

    @Test
    void deleteBusinessRemovesOwnedBusinessAndAssociatedData() throws Exception {
        Business business = new Business();
        business.setId(42L);
        business.setName("Mi Restaurante");
        business.setType("RESTAURANT");
        business.setOwnerId(7L);

        User owner = new User();
        owner.setId(7L);
        owner.setUsername("owner@example.com");

        Resource resource = new Resource();
        resource.setId(9L);
        resource.setBusiness(business);

        Reservation reservation = new Reservation();
        reservation.setId(11L);
        reservation.setResource(resource);

        when(businessRepo.findById(42L)).thenReturn(Optional.of(business));
        when(userRepo.findByUsername("owner@example.com")).thenReturn(Optional.of(owner));
        when(resourceRepo.findByBusinessId(42L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(resource)));
        when(reservationRepo.findByResourceId(9L)).thenReturn(List.of(reservation));

        UserDetails principalUser = org.springframework.security.core.userdetails.User.withUsername("owner@example.com")
                .password("secret")
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"))
                .build();

        mockMvc.perform(delete("/v1/businesses/42")
                        .principal(new UsernamePasswordAuthenticationToken(principalUser, "secret", principalUser.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true))
                .andExpect(jsonPath("$.businessId").value(42));

        verify(reservationRepo).deleteAll(List.of(reservation));
        verify(resourceRepo).deleteAll(List.of(resource));
        verify(businessRepo).delete(business);
    }

    @Test
    void mineReturnsOnlyBusinessesOwnedByAuthenticatedUser() throws Exception {
        Business business = new Business();
        business.setId(42L);
        business.setName("Mi Restaurante");
        business.setType("RESTAURANT");

        User owner = new User();
        owner.setId(7L);
        owner.setUsername("owner@example.com");

        when(userRepo.findByUsername("owner@example.com")).thenReturn(Optional.of(owner));
        when(businessRepo.findByOwnerId(7L)).thenReturn(List.of(business));

        UserDetails principalUser = org.springframework.security.core.userdetails.User.withUsername("owner@example.com")
                .password("secret")
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"))
                .build();

        mockMvc.perform(get("/v1/businesses/mine")
                        .principal(new UsernamePasswordAuthenticationToken(principalUser, "secret", principalUser.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].name").value("Mi Restaurante"));
    }
}
