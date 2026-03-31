package ru.vova.airbnb.security.jwt;

import io.jsonwebtoken.Claims;
import ru.vova.airbnb.entity.UserRole;
import ru.vova.airbnb.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String email;
    @JsonIgnore
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean enabled;
    private Boolean verified;

    public static UserDetailsImpl build(User user) {
        return UserDetailsImpl.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .verified(user.getVerified())
                .build();
    }

    public static UserDetailsImpl fromClaims(Claims claims) {
        return UserDetailsImpl.builder()
                .id(Long.parseLong(claims.getSubject()))
                .email(claims.get("email", String.class))
                .firstName(claims.get("firstName", String.class))
                .lastName(claims.get("lastName", String.class))
                .role(claims.get("role", String.class))
                .enabled(claims.get("enabled", Boolean.class))
                .verified(claims.get("verified", Boolean.class))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

        UserRole userRole = UserRole.valueOf(role);
        userRole.getPrivileges().forEach(privilege ->
                authorities.add(new SimpleGrantedAuthority(privilege.name()))
        );

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVerified() {return verified;}
}