package edu.minervia.platform.config

import edu.minervia.platform.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket configuration using Spring SimpleBroker (in-memory)
 * per CONSTRAINT [STOMP-SIMPLE-BROKER].
 *
 * - No external message broker required
 * - Suitable for <50 registrations/day
 * - No persistence: messages lost on disconnect
 * - JWT authentication on WebSocket handshake
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtService: JwtService
) : WebSocketMessageBrokerConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // Enable simple broker for topics
        registry.enableSimpleBroker("/topic")
        // Application destination prefix
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // WebSocket endpoint with SockJS fallback
        // TODO: Restrict origins in production
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(
                "http://localhost:*",
                "https://localhost:*",
                "https://*.minervia.edu.pl"
            )
            .withSockJS()

        // Plain WebSocket endpoint
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(
                "http://localhost:*",
                "https://localhost:*",
                "https://*.minervia.edu.pl"
            )
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(JwtChannelInterceptor(jwtService))
    }

    /**
     * Intercept STOMP CONNECT to validate JWT token.
     */
    inner class JwtChannelInterceptor(
        private val jwtService: JwtService
    ) : ChannelInterceptor {

        override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
            val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

            if (accessor?.command == StompCommand.CONNECT) {
                val authHeader = accessor.getFirstNativeHeader("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    val token = authHeader.substring(7)
                    try {
                        val claims = jwtService.validateAccessToken(token)
                        val username = claims.subject
                        val role = claims["role"] as? String ?: "USER"

                        val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                        val authentication = UsernamePasswordAuthenticationToken(
                            username, null, authorities
                        )
                        accessor.user = authentication
                        log.debug("WebSocket authenticated: {}", username)
                    } catch (e: Exception) {
                        log.warn("WebSocket authentication failed: {}", e.message)
                        // Allow connection but without authentication
                        // Subscriptions to protected topics will be rejected
                    }
                }
            }

            return message
        }
    }
}
