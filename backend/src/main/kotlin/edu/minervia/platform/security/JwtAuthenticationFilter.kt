package edu.minervia.platform.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: AdminUserDetailsService,
    private val tokenRevocationService: TokenRevocationService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        val username = jwtService.getUsernameFromToken(jwt)

        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            if (jwtService.validateToken(jwt)) {
                // CONSTRAINT [JWT-REVOCATION-LIST]: Check revocation list after signature verification
                val jti = jwtService.getJtiFromToken(jwt)
                if (jti != null && tokenRevocationService.isRevoked(jti)) {
                    filterChain.doFilter(request, response)
                    return
                }

                // Only accept ACCESS tokens for API authentication
                val tokenType = jwtService.getTokenTypeFromToken(jwt)
                if (tokenType != TokenType.ACCESS) {
                    filterChain.doFilter(request, response)
                    return
                }

                val userDetails = userDetailsService.loadUserByUsername(username)
                if (userDetails.isEnabled && userDetails.isAccountNonLocked) {
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
