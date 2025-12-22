package com.orangeserver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/departments")
public class ApiDepartments extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ApiDepartments.class);
    private static final Pattern STRING_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern NULL_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*null", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");


        /*
         * { "deptCode": "D001", "deptName": "行政部", "remark": "可选" }
         */
        String body = ApiUtils.readBody(req);
        String deptCode = ApiUtils.extractString(body, "deptCode", STRING_FIELD);
        String deptName = ApiUtils.extractString(body, "deptName", STRING_FIELD);
        String remark = ApiUtils.extractNullableString(body, "remark", STRING_FIELD, NULL_FIELD);

        if (ApiUtils.isBlank(deptCode) || ApiUtils.isBlank(deptName)) {
            ApiUtils.writeJson(resp, 4001, "deptCode 和 deptName 为必填字段", "null");
            return;
        }

        String sql = "INSERT INTO department (dept_code, dept_name, remark) VALUES (?, ?, ?)";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, deptCode);
            ps.setString(2, deptName);
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(3, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(3, remark);
            }
            ps.executeUpdate();

            long id = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }

            String data = "{ \"id\": " + id
                    + ", \"deptCode\": \"" + ApiUtils.escapeJson(deptCode)
                    + "\", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("新增部门成功 deptCode={} deptName={}", deptCode, deptName);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "deptCode 已存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("新增部门失败", e);
        }
    }
}
