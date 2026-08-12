package com.reservas.gateway.filter;

import com.reservas.gateway.security.GatewayJwtValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Primer punto de rechazo para requests sin JWT valido. Cada servicio downstream
 * vuelve a validar el token de forma independiente (defensa en profundidad) -
 * este filtro solo evita que trafico no autenticado llegue a la red interna.
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewayJwtValidator jwtValidator;

    public JwtAuthGlobalFilter(GatewayJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublic(request.getMethod(), path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = (authHeader != null && authHeader.startsWith(BEARER_PREFIX))
                ? authHeader.substring(BEARER_PREFIX.length())
                : null;

        if (token == null || !jwtValidator.isValid(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        return HttpMethod.GET.equals(method) && path.startsWith("/api/resources");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
