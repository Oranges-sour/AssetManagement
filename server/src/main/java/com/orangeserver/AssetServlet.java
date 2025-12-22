package com.orangeserver;

import java.io.IOException;
import java.math.BigDecimal;
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

@WebServlet("/api/assets/*")
public class AssetServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AssetServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.isEmpty()) {
            if (pathInfo.matches("/\\d+/assign/?")) {
                Long id = ApiUtils.parseId(pathInfo.replaceAll("/assign/?", ""));
                if (id == null) {
                    ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
                    return;
                }
                handleAssign(req, resp, id);
                return;
            }
            if (pathInfo.matches("/\\d+/return/?")) {
                Long id = ApiUtils.parseId(pathInfo.replaceAll("/return/?", ""));
                if (id == null) {
                    ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
                    return;
                }
                handleReturn(resp, id);
                return;
            }
        }

        handleCreate(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            handleList(req, resp);
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
        String assetNo = ApiUtils.extractString(body, "assetNo", ApiUtils.STRING_FIELD);
        String assetName = ApiUtils.extractString(body, "assetName", ApiUtils.STRING_FIELD);
        BigDecimal value = ApiUtils.extractDecimal(body, "value");
        Long locationId = ApiUtils.extractLong(body, "locationId");
        Long assigneeId = ApiUtils.extractNullableLong(body, "assigneeId");
        String remark = ApiUtils.extractNullableString(body, "remark", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);

        if (ApiUtils.isBlank(assetNo) || ApiUtils.isBlank(assetName) || value == null || locationId == null) {
            ApiUtils.writeJson(resp, 4001, "assetNo、assetName、value、locationId 为必填字段", "null");
            return;
        }

        int status = assigneeId == null ? 0 : 1;
        String sql = "UPDATE asset SET asset_no = ?, asset_name = ?, value = ?, location_id = ?,"
                + " assignee_id = ?, status = ?, remark = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assetNo);
            ps.setString(2, assetName);
            ps.setBigDecimal(3, value);
            ps.setLong(4, locationId);
            if (assigneeId == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                ps.setLong(5, assigneeId);
            }
            ps.setInt(6, status);
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(7, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(7, remark);
            }
            ps.setLong(8, id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                ApiUtils.writeJson(resp, 4004, "资产不存在", "null");
                return;
            }

            String data = "{ \"id\": " + id
                    + ", \"assetNo\": \"" + ApiUtils.escapeJson(assetNo) + "\""
                    + ", \"assetName\": \"" + ApiUtils.escapeJson(assetName) + "\""
                    + ", \"value\": " + value.toPlainString()
                    + ", \"locationId\": " + locationId
                    + ", \"assigneeId\": " + (assigneeId == null ? "null" : assigneeId)
                    + ", \"status\": " + status
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("修改资产成功 id={} assetNo={}", id, assetNo);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "assetNo 已存在", "null");
            } else if (e.getErrorCode() == 1452) {
                ApiUtils.writeJson(resp, 4004, "位置空间或领用人不存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("修改资产失败 id={}", id, e);
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

        String deleteSql = "DELETE FROM asset WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement del = conn.prepareStatement(deleteSql)) {
            del.setLong(1, id);
            int deleted = del.executeUpdate();
            if (deleted == 0) {
                ApiUtils.writeJson(resp, 4004, "资产不存在", "null");
                return;
            }
            ApiUtils.writeJson(resp, 0, "ok", "null");
            logger.info("删除资产成功 id={}", id);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("删除资产失败 id={}", id, e);
        }
    }

    private void handleCreate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        /*
         * { "assetNo": "AS0001", "assetName": "笔记本电脑", "value": 8000.00,
         *   "locationId": 10, "assigneeId": null, "remark": "" }
         */
        String body = ApiUtils.readBody(req);
        String assetNo = ApiUtils.extractString(body, "assetNo", ApiUtils.STRING_FIELD);
        String assetName = ApiUtils.extractString(body, "assetName", ApiUtils.STRING_FIELD);
        BigDecimal value = ApiUtils.extractDecimal(body, "value");
        Long locationId = ApiUtils.extractLong(body, "locationId");
        Long assigneeId = ApiUtils.extractNullableLong(body, "assigneeId");
        String remark = ApiUtils.extractNullableString(body, "remark", ApiUtils.STRING_FIELD, ApiUtils.NULL_FIELD);

        if (ApiUtils.isBlank(assetNo) || ApiUtils.isBlank(assetName) || value == null || locationId == null) {
            ApiUtils.writeJson(resp, 4001, "assetNo、assetName、value、locationId 为必填字段", "null");
            return;
        }

        int status = assigneeId == null ? 0 : 1;
        String sql = "INSERT INTO asset (asset_no, asset_name, value, location_id, assignee_id, status, remark)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, assetNo);
            ps.setString(2, assetName);
            ps.setBigDecimal(3, value);
            ps.setLong(4, locationId);
            if (assigneeId == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                ps.setLong(5, assigneeId);
            }
            ps.setInt(6, status);
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(7, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(7, remark);
            }
            ps.executeUpdate();

            long id = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }

            String data = "{ \"id\": " + id
                    + ", \"assetNo\": \"" + ApiUtils.escapeJson(assetNo) + "\""
                    + ", \"assetName\": \"" + ApiUtils.escapeJson(assetName) + "\""
                    + ", \"value\": " + value.toPlainString()
                    + ", \"locationId\": " + locationId
                    + ", \"assigneeId\": " + (assigneeId == null ? "null" : assigneeId)
                    + ", \"status\": " + status
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("新增资产成功 assetNo={} assetName={}", assetNo, assetName);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "assetNo 已存在", "null");
            } else if (e.getErrorCode() == 1452) {
                ApiUtils.writeJson(resp, 4004, "位置空间或领用人不存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("新增资产失败", e);
        }
    }

    private void handleAssign(HttpServletRequest req, HttpServletResponse resp, long id) throws IOException {
        String body = ApiUtils.readBody(req);
        Long assigneeId = ApiUtils.extractLong(body, "assigneeId");
        if (assigneeId == null) {
            ApiUtils.writeJson(resp, 4001, "assigneeId 为必填字段", "null");
            return;
        }

        String assetSql = "SELECT status FROM asset WHERE id = ?";
        String assigneeSql = "SELECT id FROM assignee WHERE id = ?";
        String updateSql = "UPDATE asset SET assignee_id = ?, status = 1 WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement assetPs = conn.prepareStatement(assetSql);
             PreparedStatement assigneePs = conn.prepareStatement(assigneeSql);
             PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
            assetPs.setLong(1, id);
            int status = -1;
            try (ResultSet rs = assetPs.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "资产不存在", "null");
                    return;
                }
                status = rs.getInt("status");
            }

            if (status == 1) {
                ApiUtils.writeJson(resp, 4002, "资产已被领用", "null");
                return;
            }

            assigneePs.setLong(1, assigneeId);
            try (ResultSet rs = assigneePs.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "领用人不存在", "null");
                    return;
                }
            }

            updatePs.setLong(1, assigneeId);
            updatePs.setLong(2, id);
            updatePs.executeUpdate();
            ApiUtils.writeJson(resp, 0, "ok", "null");
            logger.info("资产领用成功 id={} assigneeId={}", id, assigneeId);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("资产领用失败 id={}", id, e);
        }
    }

    private void handleReturn(HttpServletResponse resp, long id) throws IOException {
        String assetSql = "SELECT status FROM asset WHERE id = ?";
        String updateSql = "UPDATE asset SET assignee_id = NULL, status = 0 WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement assetPs = conn.prepareStatement(assetSql);
             PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
            assetPs.setLong(1, id);
            int status = -1;
            try (ResultSet rs = assetPs.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "资产不存在", "null");
                    return;
                }
                status = rs.getInt("status");
            }

            if (status == 0) {
                ApiUtils.writeJson(resp, 4002, "资产已处于闲置", "null");
                return;
            }

            updatePs.setLong(1, id);
            updatePs.executeUpdate();
            ApiUtils.writeJson(resp, 0, "ok", "null");
            logger.info("资产归还成功 id={}", id);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("资产归还失败 id={}", id, e);
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        String deptIdValue = req.getParameter("deptId");
        String locationIdValue = req.getParameter("locationId");
        String assigneeIdValue = req.getParameter("assigneeId");
        String statusValue = req.getParameter("status");

        Long deptId = ApiUtils.parseLongParam(deptIdValue);
        Long locationId = ApiUtils.parseLongParam(locationIdValue);
        Long assigneeId = ApiUtils.parseLongParam(assigneeIdValue);
        Integer status = ApiUtils.parseIntParam(statusValue);
        if ((deptId == null && !ApiUtils.isBlank(deptIdValue))
                || (locationId == null && !ApiUtils.isBlank(locationIdValue))
                || (assigneeId == null && !ApiUtils.isBlank(assigneeIdValue))) {
            ApiUtils.writeJson(resp, 4001, "筛选参数格式不正确", "null");
            return;
        }
        if (statusValue != null && !statusValue.isEmpty() && status == null) {
            ApiUtils.writeJson(resp, 4001, "status 格式不正确", "null");
            return;
        }
        if (status != null && status != 0 && status != 1) {
            ApiUtils.writeJson(resp, 4001, "status 需为 0 或 1", "null");
            return;
        }

        int page = ApiUtils.parseInt(req.getParameter("page"), 1);
        int size = ApiUtils.parseInt(req.getParameter("size"), 10);
        if (page <= 0 || size <= 0) {
            ApiUtils.writeJson(resp, 4001, "page 和 size 需为正整数", "null");
            return;
        }

        boolean hasKeyword = !ApiUtils.isBlank(keyword);
        List<String> conditions = new ArrayList<>();
        if (deptId != null) {
            conditions.add("ls.dept_id = ?");
        }
        if (locationId != null) {
            conditions.add("ast.location_id = ?");
        }
        if (assigneeId != null) {
            conditions.add("ast.assignee_id = ?");
        }
        if (status != null) {
            conditions.add("ast.status = ?");
        }
        if (hasKeyword) {
            conditions.add("(ast.asset_no LIKE ? OR ast.asset_name LIKE ?)");
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM asset ast"
                + " JOIN location_space ls ON ast.location_id = ls.id"
                + " JOIN department d ON ls.dept_id = d.id"
                + " LEFT JOIN assignee ag ON ast.assignee_id = ag.id"
                + where;
        String listSql = "SELECT ast.id, ast.asset_no, ast.asset_name, ast.value, ast.location_id,"
                + " ast.assignee_id, ast.status, ast.remark, ls.room_no, ls.dept_id, d.dept_name,"
                + " ag.name AS assignee_name"
                + " FROM asset ast"
                + " JOIN location_space ls ON ast.location_id = ls.id"
                + " JOIN department d ON ls.dept_id = d.id"
                + " LEFT JOIN assignee ag ON ast.assignee_id = ag.id"
                + where
                + " ORDER BY ast.id DESC LIMIT ? OFFSET ?";

        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement listPs = conn.prepareStatement(listSql)) {
            String like = hasKeyword ? "%" + keyword + "%" : null;
            int index = 1;
            if (deptId != null) {
                countPs.setLong(index, deptId);
                listPs.setLong(index, deptId);
                index++;
            }
            if (locationId != null) {
                countPs.setLong(index, locationId);
                listPs.setLong(index, locationId);
                index++;
            }
            if (assigneeId != null) {
                countPs.setLong(index, assigneeId);
                listPs.setLong(index, assigneeId);
                index++;
            }
            if (status != null) {
                countPs.setInt(index, status);
                listPs.setInt(index, status);
                index++;
            }
            if (hasKeyword) {
                countPs.setString(index, like);
                countPs.setString(index + 1, like);
                listPs.setString(index, like);
                listPs.setString(index + 1, like);
                index += 2;
            }
            listPs.setInt(index, size);
            listPs.setInt(index + 1, (page - 1) * size);

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
                    BigDecimal value = rs.getBigDecimal("value");
                    long locationIdResult = rs.getLong("location_id");
                    Object assigneeObj = rs.getObject("assignee_id");
                    Long assigneeIdResult = assigneeObj == null ? null : ((Number) assigneeObj).longValue();
                    int statusResult = rs.getInt("status");
                    String remark = rs.getString("remark");
                    String roomNo = rs.getString("room_no");
                    long deptIdResult = rs.getLong("dept_id");
                    String deptName = rs.getString("dept_name");
                    String assigneeName = rs.getString("assignee_name");
                    String item = "{ \"id\": " + id
                            + ", \"assetNo\": \"" + ApiUtils.escapeJson(assetNo) + "\""
                            + ", \"assetName\": \"" + ApiUtils.escapeJson(assetName) + "\""
                            + ", \"value\": " + (value == null ? "0" : value.toPlainString())
                            + ", \"locationId\": " + locationIdResult
                            + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                            + ", \"deptId\": " + deptIdResult
                            + ", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                            + ", \"assigneeId\": " + (assigneeIdResult == null ? "null" : assigneeIdResult)
                            + ", \"assigneeName\": " + (assigneeName == null ? "null"
                            : "\"" + ApiUtils.escapeJson(assigneeName) + "\"")
                            + ", \"status\": " + statusResult
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
            logger.error("查询资产列表失败", e);
        }
    }

    private void handleDetail(HttpServletResponse resp, long id) throws IOException {
        String sql = "SELECT ast.id, ast.asset_no, ast.asset_name, ast.value, ast.location_id,"
                + " ast.assignee_id, ast.status, ast.remark, ls.room_no, ls.dept_id, d.dept_name,"
                + " ag.name AS assignee_name"
                + " FROM asset ast"
                + " JOIN location_space ls ON ast.location_id = ls.id"
                + " JOIN department d ON ls.dept_id = d.id"
                + " LEFT JOIN assignee ag ON ast.assignee_id = ag.id"
                + " WHERE ast.id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "资产不存在", "null");
                    return;
                }
                String assetNo = rs.getString("asset_no");
                String assetName = rs.getString("asset_name");
                BigDecimal value = rs.getBigDecimal("value");
                long locationId = rs.getLong("location_id");
                Object assigneeObj = rs.getObject("assignee_id");
                Long assigneeId = assigneeObj == null ? null : ((Number) assigneeObj).longValue();
                int status = rs.getInt("status");
                String remark = rs.getString("remark");
                String roomNo = rs.getString("room_no");
                long deptId = rs.getLong("dept_id");
                String deptName = rs.getString("dept_name");
                String assigneeName = rs.getString("assignee_name");
                String data = "{ \"id\": " + id
                        + ", \"assetNo\": \"" + ApiUtils.escapeJson(assetNo) + "\""
                        + ", \"assetName\": \"" + ApiUtils.escapeJson(assetName) + "\""
                        + ", \"value\": " + (value == null ? "0" : value.toPlainString())
                        + ", \"locationId\": " + locationId
                        + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                        + ", \"deptId\": " + deptId
                        + ", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                        + ", \"assigneeId\": " + (assigneeId == null ? "null" : assigneeId)
                        + ", \"assigneeName\": " + (assigneeName == null ? "null"
                        : "\"" + ApiUtils.escapeJson(assigneeName) + "\"")
                        + ", \"status\": " + status
                        + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                        + " }";
                ApiUtils.writeJson(resp, 0, "ok", data);
            }
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("查询资产详情失败 id={}", id, e);
        }
    }

}
