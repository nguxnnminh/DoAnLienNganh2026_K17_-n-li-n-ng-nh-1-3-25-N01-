package com.shop.clothingstore.controller.api;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.entity.WishlistItem;
import com.shop.clothingstore.security.JwtUtil;
import com.shop.clothingstore.service.CustomUserDetailsService;
import com.shop.clothingstore.service.UserService;
import com.shop.clothingstore.service.WishlistService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class WishlistApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // =============================================================
    // GET /api/wishlist
    // =============================================================
    @Test
    @DisplayName("GET /api/wishlist returns items for authenticated user")
    void getWishlist_Authenticated_ReturnsItems() throws Exception {
        User user = mockUser();
        Product product = new Product();
        injectId(product, 10L);
        product.setName("Essential Tee");

        WishlistItem item = new WishlistItem();
        injectId(item, 100L);
        item.setUser(user);
        item.setProduct(product);

        when(userService.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));
        when(wishlistService.getWishlist(user)).thenReturn(List.of(item));

        Authentication auth = new UsernamePasswordAuthenticationToken("user@test.com", null);

        mockMvc.perform(get("/api/wishlist").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L));
    }

    @Test
    @DisplayName("GET /api/wishlist without auth returns 401")
    void getWishlist_NoAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    // =============================================================
    // POST /api/wishlist/{productId}
    // =============================================================
    @Test
    @DisplayName("POST /api/wishlist/{productId} adds to wishlist")
    void addToWishlist_Authenticated_ReturnsOk() throws Exception {
        User user = mockUser();
        when(userService.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));
        doNothing().when(wishlistService).addToWishlist(user, 10L);

        Authentication auth = new UsernamePasswordAuthenticationToken("user@test.com", null);

        mockMvc.perform(post("/api/wishlist/10").principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/wishlist/{productId} without auth returns 401")
    void addToWishlist_NoAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/wishlist/10"))
                .andExpect(status().isUnauthorized());
    }

    // =============================================================
    // DELETE /api/wishlist/{productId}
    // =============================================================
    @Test
    @DisplayName("DELETE /api/wishlist/{productId} removes from wishlist")
    void removeFromWishlist_Authenticated_ReturnsOk() throws Exception {
        User user = mockUser();
        when(userService.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));
        doNothing().when(wishlistService).removeFromWishlist(user, 10L);

        Authentication auth = new UsernamePasswordAuthenticationToken("user@test.com", null);

        mockMvc.perform(delete("/api/wishlist/10").principal(auth))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/wishlist/{productId} without auth returns 401")
    void removeFromWishlist_NoAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/wishlist/10"))
                .andExpect(status().isUnauthorized());
    }

    // =============================================================
    // Helpers
    // =============================================================

    /** Build a mock user. setId() is protected in BaseEntity — use reflection. */
    private User mockUser() {
        User user = new User();
        injectId(user, 1L);
        user.setEmail("user@test.com");
        user.setFullName("Test User");
        user.setRole(Role.USER);
        return user;
    }

    /**
     * Inject an ID into a BaseEntity via reflection.
     * Required because setId() is protected in BaseEntity and the test class
     * is in a different package (not a subclass of BaseEntity).
     */
    private static void injectId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field =
                    com.shop.clothingstore.entity.base.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject ID in test", e);
        }
    }
}
