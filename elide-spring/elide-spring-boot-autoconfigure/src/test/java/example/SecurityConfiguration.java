/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Disables security for testing. Adds filter and user logic for Async Testing
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests().antMatchers("/**").permitAll().and().csrf().disable()
                .addFilterAfter(new SecurityConfiguration.DemoAuthFilter(), ConcurrentSessionFilter.class);
    }

    /**
     * Example Auth filter implementation for Elide Spring Boot Testing to use async query.
     * This implementation fakes a Test user.
     * Please replace with your implementation when using this class.
     */
    class DemoAuthFilter extends GenericFilterBean {

        @Override
        public void doFilter(
                ServletRequest request,
                ServletResponse response,
                FilterChain chain) throws IOException, ServletException {

            SecurityConfiguration.DemoAuthFilter.ElideSpringUser user = new SecurityConfiguration.DemoAuthFilter.ElideSpringUser("test", "{noop}test", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user, user.getPassword(), new ArrayList<>());

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authRequest);

            chain.doFilter(request, response);
        }

        /**
         * Example User.
         */
        class ElideSpringUser extends org.springframework.security.core.userdetails.User {

            private static final long serialVersionUID = 1L;
            private String name;

            public ElideSpringUser(String username, String password, Collection<? extends GrantedAuthority> authorities) {
                super(username, password,  authorities);
                this.name = username;
            }

            public String getName() {
                return name;
            }
        }
    }
}
