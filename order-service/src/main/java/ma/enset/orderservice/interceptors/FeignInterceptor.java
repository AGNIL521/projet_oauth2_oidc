package ma.enset.orderservice.interceptors;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication instanceof JwtAuthenticationToken){
            JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
            String token = jwtAuthenticationToken.getToken().getTokenValue();
            requestTemplate.header("Authorization", "Bearer " + token);
        }
    }
}
