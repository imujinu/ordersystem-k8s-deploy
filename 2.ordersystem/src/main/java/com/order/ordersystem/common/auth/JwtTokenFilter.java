package com.order.ordersystem.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenFilter extends GenericFilter {

    @Value("${jwt.secretKeyAt}")
    private String secretKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
    try{
        HttpServletRequest req = (HttpServletRequest)request;
        String bearerToken = req.getHeader("Authorization");
        if(bearerToken==null){
            filterChain.doFilter(request,response);
            return;
        }

        String token = bearerToken.substring(7);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        List<GrantedAuthority> authorityList = new ArrayList<>();
        authorityList.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), "", authorityList);
        SecurityContextHolder.getContext().setAuthentication(authentication);

    }catch(Exception e){


    }

    filterChain.doFilter(request,response);
     }
}
