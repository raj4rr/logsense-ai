package com.logsense.security;
import com.logsense.config.LogSenseProperties;import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.web.*;import org.springframework.security.oauth2.jwt.JwtDecoders;import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,ApiKeyFilter apiKeyFilter,LogSenseProperties p)throws Exception{
  http.csrf(c->c.disable())
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/actuator/health/**","/error","/","/index.html","/static/**","/api/v1/chat/**").permitAll().anyRequest().authenticated())
      .addFilterBefore(apiKeyFilter,UsernamePasswordAuthenticationFilter.class);
  if(p.security()!=null&&p.security().issuerUri()!=null&&!p.security().issuerUri().isBlank())
      http.oauth2ResourceServer(o->o.jwt(j->j.decoder(JwtDecoders.fromIssuerLocation(p.security().issuerUri()))));
  return http.build();
 }
}
