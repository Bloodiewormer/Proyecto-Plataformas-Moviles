package cr.ac.una.glifo.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import java.time.Instant
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
    private val corsConfigurationSource: CorsConfigurationSource,
    @Value("\${url.auth}") private val authUrl: String,
    @Value("\${url.health}") private val healthUrl: String
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
                    "$authUrl/register",
                    "$authUrl/login",
                    healthUrl,
                    "/error"
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { handling ->
                // Sin token (o con uno vencido) el cliente recibe 401, que es lo que la app
                // interpreta como "la sesión expiró"; el 403 queda para "no tenés permiso".
                handling.authenticationEntryPoint { request, response, _ ->
                    writeError(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tu sesión expiró. Iniciá sesión de nuevo.")
                }
                handling.accessDeniedHandler { request, response, _ ->
                    writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "No tenés permiso para realizar esta acción")
                }
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /**
     * Mismo formato ApiError que GlobalExceptionHandler. Se escribe a mano porque el error
     * ocurre en el filtro de seguridad, antes de llegar a un controller y a su advice.
     */
    private fun writeError(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String
    ) {
        response.status = status.value()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write(
            """{"status":${status.value()},"error":"${status.reasonPhrase}","code":"$code",""" +
                """"message":"$message","timestamp":"${Instant.now()}","path":"${request.requestURI}"}"""
        )
    }
}
