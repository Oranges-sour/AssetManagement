package com.orangeserver;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HelloServlet - a simple servlet for demo.
 */
@WebServlet("/api/health")
public class HelloServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HelloServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().println("{ \"code\": 0, \"msg\": \"ok\", \"data\": { \"status\": \"UP\" } }");

        logger.info("/api/health 收到指令");
    }
}
