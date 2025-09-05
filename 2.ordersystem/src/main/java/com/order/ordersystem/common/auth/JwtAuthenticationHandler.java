package com.order.ordersystem.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.ordersystem.common.dto.CommonErrorDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
@Slf4j
public class JwtAuthenticationHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        System.out.println("변경사항 테스트");
        CommonErrorDto dto = new CommonErrorDto(401, "token이 없거나 유효하지 다.");

        PrintWriter printWriter = response.getWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        String body = objectMapper.writeValueAsString(dto);

        printWriter.write(body);
        printWriter.flush();
    }
}
