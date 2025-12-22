package com.orangeserver;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/departments/*")
public class ApiDepartments extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ApiDepartments.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");


        /*
         * { "deptCode": "D001", "deptName": "行政部", "remark": "可选" }
         */
        String body = ApiUtils.readBody(req);
        String deptCode = ApiUtils.extractString(body, "deptCode", ApiUtils.STRING_FIELD);
        String deptName = ApiUtils.extractString(body, "deptName", ApiUtils.STRING_FIELD);
        String remark = ApiUtils.extractNullableString(body, "remark", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // logger.info("收到get请求");
        
        resp.setContentType("application/json; charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            handleList(req, resp);
            return;
        }

        if (pathInfo.matches("/\\d+/locations/?")) {
            Long deptId = ApiUtils.parseId(pathInfo.replaceAll("/locations/?", ""));
            if (deptId == null) {
                ApiUtils.writeJson(resp, 4001, "deptId 格式不正确", "null");
                return;
            }
            handleDeptLocations(resp, deptId);
            return;
        }

        Long id = ApiUtils.parseId(pathInfo);
        if (id == null) {
            ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
            return;
        }
        handleDetail(resp, id);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        Long id = ApiUtils.parseId(req.getPathInfo());
        if (id == null) {
            ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
            return;
        }

        String body = ApiUtils.readBody(req);
        String deptCode = ApiUtils.extractString(body, "deptCode", ApiUtils.STRING_FIELD);
        String deptName = ApiUtils.extractString(body, "deptName", ApiUtils.STRING_FIELD);
        String remark = ApiUtils.extractNullableString(body, "remark", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);

        if (ApiUtils.isBlank(deptCode) || ApiUtils.isBlank(deptName)) {
            ApiUtils.writeJson(resp, 4001, "deptCode 和 deptName 为必填字段", "null");
            return;
        }

        String sql = "UPDATE department SET dept_code = ?, dept_name = ?, remark = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deptCode);
            ps.setString(2, deptName);
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(3, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(3, remark);
            }
            ps.setLong(4, id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                ApiUtils.writeJson(resp, 4004, "部门不存在", "null");
                return;
            }

            String data = "{ \"id\": " + id
                    + ", \"deptCode\": \"" + ApiUtils.escapeJson(deptCode)
                    + "\", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("修改部门成功 id={} deptCode={} deptName={}", id, deptCode, deptName);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "deptCode 已存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("修改部门失败 id={}", id, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        Long id = ApiUtils.parseId(req.getPathInfo());
        if (id == null) {
            ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
            return;
        }

        String checkSql = "SELECT COUNT(*) FROM location_space WHERE dept_id = ?";
        String deleteSql = "DELETE FROM department WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql);
             PreparedStatement del = conn.prepareStatement(deleteSql)) {
            check.setLong(1, id);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    ApiUtils.writeJson(resp, 4002, "部门下存在位置空间，禁止删除", "null");
                    return;
                }
            }

            del.setLong(1, id);
            int deleted = del.executeUpdate();
            if (deleted == 0) {
                ApiUtils.writeJson(resp, 4004, "部门不存在", "null");
                return;
            }

            ApiUtils.writeJson(resp, 0, "ok", "null");
            logger.info("删除部门成功 id={}", id);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("删除部门失败 id={}", id, e);
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        int page = ApiUtils.parseInt(req.getParameter("page"), 1);
        int size = ApiUtils.parseInt(req.getParameter("size"), 10);
        if (page <= 0 || size <= 0) {
            ApiUtils.writeJson(resp, 4001, "page 和 size 需为正整数", "null");
            return;
        }

        boolean hasKeyword = !ApiUtils.isBlank(keyword);
        String countSql = "SELECT COUNT(*) FROM department"
                + (hasKeyword ? " WHERE dept_code LIKE ? OR dept_name LIKE ?" : "");
        String listSql = "SELECT id, dept_code, dept_name, remark FROM department"
                + (hasKeyword ? " WHERE dept_code LIKE ? OR dept_name LIKE ?" : "")
                + " ORDER BY id DESC LIMIT ? OFFSET ?";

        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement listPs = conn.prepareStatement(listSql)) {
            if (hasKeyword) {
                String like = "%" + keyword + "%";
                countPs.setString(1, like);
                countPs.setString(2, like);
                listPs.setString(1, like);
                listPs.setString(2, like);
                listPs.setInt(3, size);
                listPs.setInt(4, (page - 1) * size);
            } else {
                listPs.setInt(1, size);
                listPs.setInt(2, (page - 1) * size);
            }

            int total = 0;
            try (ResultSet rs = countPs.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            List<String> items = new ArrayList<>();
            try (ResultSet rs = listPs.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String deptCode = rs.getString("dept_code");
                    String deptName = rs.getString("dept_name");
                    String remark = rs.getString("remark");
                    String item = "{ \"id\": " + id
                            + ", \"deptCode\": \"" + ApiUtils.escapeJson(deptCode)
                            + "\", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                            + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                            + " }";
                    items.add(item);
                }
            }

            String data = "{ \"list\": [" + String.join(", ", items) + "]"
                    + ", \"page\": " + page
                    + ", \"size\": " + size
                    + ", \"total\": " + total
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("查询部门列表失败", e);
        }
    }

    private void handleDetail(HttpServletResponse resp, long id) throws IOException {
        String sql = "SELECT id, dept_code, dept_name, remark FROM department WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "部门不存在", "null");
                    return;
                }
                String deptCode = rs.getString("dept_code");
                String deptName = rs.getString("dept_name");
                String remark = rs.getString("remark");
                String data = "{ \"id\": " + id
                        + ", \"deptCode\": \"" + ApiUtils.escapeJson(deptCode)
                        + "\", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                        + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                        + " }";
                ApiUtils.writeJson(resp, 0, "ok", data);
            }
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("查询部门详情失败 id={}", id, e);
        }
    }

    private void handleDeptLocations(HttpServletResponse resp, long deptId) throws IOException {
        String sql = "SELECT id, room_no FROM location_space WHERE dept_id = ? ORDER BY id DESC";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, deptId);
            List<String> items = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String roomNo = rs.getString("room_no");
                    String item = "{ \"id\": " + id
                            + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\" }";
                    items.add(item);
                }
            }
            ApiUtils.writeJson(resp, 0, "ok", "[" + String.join(", ", items) + "]");
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("查询部门位置空间失败 deptId={}", deptId, e);
        }
    }

}
