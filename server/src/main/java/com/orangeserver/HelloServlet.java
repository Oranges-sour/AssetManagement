package com.orangeserver;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HelloServlet - a simple servlet for demo.
 */
@WebServlet("/api/health")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().println("{ \"code\": 0, \"msg\": \"ok\", \"data\": { \"status\": \"UP\" } }");
    }
}
