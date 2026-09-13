package nguyen.vn.spring_admin.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import nguyen.vn.spring_admin.entity.User;
import nguyen.vn.spring_admin.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repository) {
        return username -> repository.findByUsernameIgnoreCase(username.trim())
                .map(SessionUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Tên đăng nhập hoặc mật khẩu không đúng."));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository repository,
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        http.authenticationProvider(provider)
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/login", "/static/**", "/favicon.ico", "/error", "/access-denied").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(login -> login.loginPage("/login").loginProcessingUrl("/login")
                        .defaultSuccessUrl("/admin", true).failureUrl("/login?error").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
                .addFilterAfter(new CurrentAccountFilter(repository), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    /** Immutable database id keeps the current account identifiable after username changes. */
    public static final class SessionUser implements UserDetails, Serializable {
        private static final long serialVersionUID = 1L;
        private final Long id;
        private final String username;
        private final String password;
        private final boolean active;
        private final String authority;

        public SessionUser(User user) {
            id = user.getId();
            username = user.getUsername();
            password = user.getPassword();
            active = user.isActive();
            authority = "ROLE_" + user.getRole().name();
        }

        public Long getId() { return id; }
        @Override public String getUsername() { return username; }
        @Override public String getPassword() { return password; }
        @Override public boolean isEnabled() { return active; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority(authority));
        }
    }

    /** An existing session must immediately observe account deletion, locks and role changes. */
    private static final class CurrentAccountFilter extends OncePerRequestFilter {
        private final UserRepository repository;
        private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

        private CurrentAccountFilter(UserRepository repository) { this.repository = repository; }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof SessionUser principal) {
                User user = repository.findById(principal.getId()).orElse(null);
                if (user == null || !user.isActive() || !Objects.equals(user.getPassword(), principal.getPassword())) {
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false) != null) request.getSession(false).invalidate();
                    response.sendRedirect(request.getContextPath() + "/login?expired");
                    return;
                }
                SessionUser refreshed = new SessionUser(user);
                if (!principal.getUsername().equals(refreshed.getUsername())
                        || !principal.getAuthorities().equals(refreshed.getAuthorities())) {
                    UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.authenticated(
                            refreshed, null, refreshed.getAuthorities());
                    token.setDetails(authentication.getDetails());
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(token);
                    SecurityContextHolder.setContext(context);
                    contexts.saveContext(context, request, response);
                }
            }
            chain.doFilter(request, response);
        }
    }
}
