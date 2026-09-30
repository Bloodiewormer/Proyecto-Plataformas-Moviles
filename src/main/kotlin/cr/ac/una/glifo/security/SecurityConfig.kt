package cr.ac.una.glifo.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val corsConfigurationSource: CorsConfigurationSource
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/health",
                    "/error"
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { handling ->
                // Sin token (o con uno vencido) el cliente recibe 401, que es lo que la app
                // interpreta como "la sesión expiró"; el 403 queda para "no tenés permiso".
                handling.authenticationEntryPoint { _, response, _ ->
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Tu sesión expiró. Iniciá sesión de nuevo.")
                }
                handling.accessDeniedHandler { _, response, _ ->
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "No tenés permiso para realizar esta acción")
                }
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /** Mismo formato de error que GlobalExceptionHandler, escrito a mano porque ocurre en el filtro de seguridad, antes de llegar a un controller. */
    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }
}
