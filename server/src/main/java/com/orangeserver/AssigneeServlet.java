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

@WebServlet("/api/assignees/*")
public class AssigneeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AssigneeServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        /*
         * { "empNo": "E1001", "name": "张三", "phone": "13800000000", "remark": "" }
         */
        String body = ApiUtils.readBody(req);
        String empNo = ApiUtils.extractString(body, "empNo", ApiUtils.STRING_FIELD);
        String name = ApiUtils.extractString(body, "name", ApiUtils.STRING_FIELD);
        String phone = ApiUtils.extractNullableString(body, "phone", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);
        String remark = ApiUtils.extractNullableString(body, "remark", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);

        if (ApiUtils.isBlank(empNo) || ApiUtils.isBlank(name)) {
            ApiUtils.writeJson(resp, 4001, "empNo 和 name 为必填字段", "null");
            return;
        }

        String sql = "INSERT INTO assignee (emp_no, name, phone, remark) VALUES (?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, empNo);
            ps.setString(2, name);
            if (ApiUtils.isBlank(phone)) {
                ps.setNull(3, java.sql.Types.VARCHAR);
                phone = null;
            } else {
                ps.setString(3, phone);
            }
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(4, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(4, remark);
            }
            ps.executeUpdate();

            long id = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }

            String data = "{ \"id\": " + id
                    + ", \"empNo\": \"" + ApiUtils.escapeJson(empNo) + "\""
                    + ", \"name\": \"" + ApiUtils.escapeJson(name) + "\""
                    + ", \"phone\": " + (phone == null ? "null" : "\"" + ApiUtils.escapeJson(phone) + "\"")
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("新增领用人成功 empNo={} name={}", empNo, name);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "empNo 已存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("新增领用人失败", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            handleList(req, resp);
            return;
        }

        if (pathInfo.matches("/\\d+/assets/?")) {
            Long assigneeId = ApiUtils.parseId(pathInfo.replaceAll("/assets/?", ""));
            if (assigneeId == null) {
                ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
                return;
            }
            handleAssigneeAssets(req, resp, assigneeId);
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
        String empNo = ApiUtils.extractString(body, "empNo", ApiUtils.STRING_FIELD);
        String name = ApiUtils.extractString(body, "name", ApiUtils.STRING_FIELD);
        String phone = ApiUtils.extractNullableString(body, "phone", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);
        String remark = ApiUtils.extractNullableString(body, "remark", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);

        if (ApiUtils.isBlank(empNo) || ApiUtils.isBlank(name)) {
            ApiUtils.writeJson(resp, 4001, "empNo 和 name 为必填字段", "null");
            return;
        }

        String sql = "UPDATE assignee SET emp_no = ?, name = ?, phone = ?, remark = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empNo);
            ps.setString(2, name);
            if (ApiUtils.isBlank(phone)) {
                ps.setNull(3, java.sql.Types.VARCHAR);
                phone = null;
            } else {
                ps.setString(3, phone);
            }
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(4, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(4, remark);
            }
            ps.setLong(5, id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                ApiUtils.writeJson(resp, 4004, "领用人不存在", "null");
                return;
            }

            String data = "{ \"id\": " + id
                    + ", \"empNo\": \"" + ApiUtils.escapeJson(empNo) + "\""
                    + ", \"name\": \"" + ApiUtils.escapeJson(name) + "\""
                    + ", \"phone\": " + (phone == null ? "null" : "\"" + ApiUtils.escapeJson(phone) + "\"")
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("修改领用人成功 id={} empNo={}", id, empNo);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "empNo 已存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("修改领用人失败 id={}", id, e);
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

        String checkSql = "SELECT COUNT(*) FROM asset WHERE assignee_id = ?";
        String deleteSql = "DELETE FROM assignee WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql);
             PreparedStatement del = conn.prepareStatement(deleteSql)) {
            check.setLong(1, id);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    ApiUtils.writeJson(resp, 4002, "领用人名下存在资产，禁止删除", "null");
                    return;
                }
            }

            del.setLong(1, id);
            int deleted = del.executeUpdate();
            if (deleted == 0) {
                ApiUtils.writeJson(resp, 4004, "领用人不存在", "null");
                return;
            }

            ApiUtils.writeJson(resp, 0, "ok", "null");
            logger.info("删除领用人成功 id={}", id);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("删除领用人失败 id={}", id, e);
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
        String countSql = "SELECT COUNT(*) FROM assignee"
                + (hasKeyword ? " WHERE emp_no LIKE ? OR name LIKE ?" : "");
        String listSql = "SELECT id, emp_no, name, phone, remark FROM assignee"
                + (hasKeyword ? " WHERE emp_no LIKE ? OR name LIKE ?" : "")
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
                    String empNo = rs.getString("emp_no");
                    String name = rs.getString("name");
                    String phone = rs.getString("phone");
                    String remark = rs.getString("remark");
                    String item = "{ \"id\": " + id
                            + ", \"empNo\": \"" + ApiUtils.escapeJson(empNo) + "\""
                            + ", \"name\": \"" + ApiUtils.escapeJson(name) + "\""
                            + ", \"phone\": " + (phone == null ? "null" : "\"" + ApiUtils.escapeJson(phone) + "\"")
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
            logger.error("查询领用人列表失败", e);
        }
    }

    private void handleDetail(HttpServletResponse resp, long id) throws IOException {
        String sql = "SELECT id, emp_no, name, phone, remark FROM assignee WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "领用人不存在", "null");
                    return;
                }
                String empNo = rs.getString("emp_no");
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String remark = rs.getString("remark");
                String data = "{ \"id\": " + id
                        + ", \"empNo\": \"" + ApiUtils.escapeJson(empNo) + "\""
                        + ", \"name\": \"" + ApiUtils.escapeJson(name) + "\""
                        + ", \"phone\": " + (phone == null ? "null" : "\"" + ApiUtils.escapeJson(phone) + "\"")
                        + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                        + " }";
                ApiUtils.writeJson(resp, 0, "ok", data);
            }
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("查询领用人详情失败 id={}", id, e);
        }
    }

    private void handleAssigneeAssets(HttpServletRequest req, HttpServletResponse resp, long assigneeId)
            throws IOException {
        int page = ApiUtils.parseInt(req.getParameter("page"), 1);
        int size = ApiUtils.parseInt(req.getParameter("size"), 10);
        if (page <= 0 || size <= 0) {
            ApiUtils.writeJson(resp, 4001, "page 和 size 需为正整数", "null");
            return;
        }

        String countSql = "SELECT COUNT(*) FROM asset WHERE assignee_id = ?";
        String listSql = "SELECT ast.id, ast.asset_no, ast.asset_name, ast.status,"
                + " ls.room_no FROM asset ast"
                + " JOIN location_space ls ON ast.location_id = ls.id"
                + " WHERE ast.assignee_id = ?"
                + " ORDER BY ast.id DESC LIMIT ? OFFSET ?";

        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement listPs = conn.prepareStatement(listSql)) {
            countPs.setLong(1, assigneeId);
            listPs.setLong(1, assigneeId);
            listPs.setInt(2, size);
            listPs.setInt(3, (page - 1) * size);

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
                    String assetNo = rs.getString("asset_no");
                    String assetName = rs.getString("asset_name");
                    int status = rs.getInt("status");
                    String roomNo = rs.getString("room_no");
                    String item = "{ \"id\": " + id
                            + ", \"assetNo\": \"" + ApiUtils.escapeJson(assetNo) + "\""
                            + ", \"assetName\": \"" + ApiUtils.escapeJson(assetName) + "\""
                            + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                            + ", \"status\": " + status
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
            logger.error("查询领用人名下资产失败 assigneeId={}", assigneeId, e);
        }
    }

}
