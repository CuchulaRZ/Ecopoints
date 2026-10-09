package backend_r.reciclaje.auth;

import backend_r.reciclaje.auth.AuthDtos.LoginRequest;
import backend_r.reciclaje.auth.AuthDtos.IdLoginRequest;
import backend_r.reciclaje.auth.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final AuthService authService;
    private final UserDetailsService userDetailsService;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CookieCsrfTokenRepository csrfTokenRepository,
            AuthService authService,
            UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.authService = authService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.identificador().trim(), request.password()));
        establishSession(authentication, httpRequest, httpResponse);
        return authService.getUser(authentication.getName());
    }

    @PostMapping("/login-id")
    public UserResponse loginById(
            @Valid @RequestBody IdLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        UserDetails user = userDetailsService.loadUserByUsername(request.identificador().trim());
        if (!user.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("Cuenta inactiva");
        }
        boolean dashboardRole = user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ALUMNO")
                        || authority.getAuthority().equals("ROLE_PROFESOR"));
        if (!dashboardRole) {
            throw new BadCredentialsException("El acceso por ID solo está disponible para alumnos y profesores");
        }
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
        establishSession(authentication, httpRequest, httpResponse);
        return authService.getUser(authentication.getName());
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return authService.getUser(authentication.getName());
    }

    private void establishSession(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();
        csrfTokenRepository.saveToken(null, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
