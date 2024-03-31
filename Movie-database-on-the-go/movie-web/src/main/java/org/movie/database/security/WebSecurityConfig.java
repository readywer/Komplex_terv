package org.movie.database.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@SuppressWarnings("ALL")
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Bean
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/css/**").permitAll()
                        .requestMatchers("/js/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/home").authenticated()
                        .requestMatchers("/client_modify").authenticated()
                        .requestMatchers("/films").authenticated()
                        .requestMatchers("/film_details").authenticated()
                        .requestMatchers("/film_watch/**").authenticated()
                        .requestMatchers("/film_add").authenticated()
                        .requestMatchers("/film_modify").authenticated()
                        .requestMatchers("/data/**").authenticated()
                        .requestMatchers("/images/**").authenticated()
                        .requestMatchers("/error").authenticated()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        .anyRequest().denyAll()

                )
                .formLogin(AbstractAuthenticationFilterConfigurer::permitAll
                );
        //for H2
        http.csrf().disable();
        http.headers().frameOptions().sameOrigin();
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
