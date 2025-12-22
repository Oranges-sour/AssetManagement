package com.orangeserver;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务心跳指令，前端检查后端是否还活着
 */
@WebServlet("/api/health")
public class ApiHealth extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(ApiHealth.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().println("{ \"code\": 0, \"msg\": \"ok\", \"data\": { \"status\": \"UP\" } }");

        logger.info("/api/health 收到服务检查指令");
    }
}
