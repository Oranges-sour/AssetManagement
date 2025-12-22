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
import java.util.regex.Pattern;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/locations/*")
public class LocationServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LocationServlet.class);
    private static final Pattern STRING_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern NULL_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*null", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*(\\d+)", Pattern.DOTALL);
    private static final Pattern DECIMAL_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)", Pattern.DOTALL);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        /*
         * { "deptId": 1, "roomNo": "A-301", "area": 60.50, "remark": "" }
         */
        String body = ApiUtils.readBody(req);
        Long deptId = extractLong(body, "deptId");
        String roomNo = ApiUtils.extractString(body, "roomNo", STRING_FIELD);
        BigDecimal area = extractDecimal(body, "area");
        String remark = ApiUtils.extractNullableString(body, "remark", STRING_FIELD, NULL_FIELD);

        if (deptId == null || ApiUtils.isBlank(roomNo) || area == null) {
            ApiUtils.writeJson(resp, 4001, "deptId、roomNo、area 为必填字段", "null");
            return;
        }

        String sql = "INSERT INTO location_space (dept_id, room_no, area, remark) VALUES (?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, deptId);
            ps.setString(2, roomNo);
            ps.setBigDecimal(3, area);
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
                    + ", \"deptId\": " + deptId
                    + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                    + ", \"area\": " + area.toPlainString()
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("新增位置空间成功 deptId={} roomNo={}", deptId, roomNo);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "房间号已存在", "null");
            } else if (e.getErrorCode() == 1452) {
                ApiUtils.writeJson(resp, 4004, "部门不存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("新增位置空间失败", e);
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

        Long id = parseId(pathInfo);
        if (id == null) {
            ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
            return;
        }
        handleDetail(resp, id);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        Long id = parseId(req.getPathInfo());
        if (id == null) {
            ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
            return;
        }

        String body = ApiUtils.readBody(req);
        Long deptId = extractLong(body, "deptId");
        String roomNo = ApiUtils.extractString(body, "roomNo", STRING_FIELD);
        BigDecimal area = extractDecimal(body, "area");
        String remark = ApiUtils.extractNullableString(body, "remark", STRING_FIELD, NULL_FIELD);

        if (deptId == null || ApiUtils.isBlank(roomNo) || area == null) {
            ApiUtils.writeJson(resp, 4001, "deptId、roomNo、area 为必填字段", "null");
            return;
        }

        String sql = "UPDATE location_space SET dept_id = ?, room_no = ?, area = ?, remark = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, deptId);
            ps.setString(2, roomNo);
            ps.setBigDecimal(3, area);
            if (ApiUtils.isBlank(remark)) {
                ps.setNull(4, java.sql.Types.VARCHAR);
                remark = null;
            } else {
                ps.setString(4, remark);
            }
            ps.setLong(5, id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                ApiUtils.writeJson(resp, 4004, "位置空间不存在", "null");
                return;
            }

            String data = "{ \"id\": " + id
                    + ", \"deptId\": " + deptId
                    + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                    + ", \"area\": " + area.toPlainString()
                    + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                    + " }";
            ApiUtils.writeJson(resp, 0, "ok", data);
            logger.info("修改位置空间成功 id={} deptId={} roomNo={}", id, deptId, roomNo);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                ApiUtils.writeJson(resp, 4090, "房间号已存在", "null");
            } else if (e.getErrorCode() == 1452) {
                ApiUtils.writeJson(resp, 4004, "部门不存在", "null");
            } else {
                ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            }
            logger.error("修改位置空间失败 id={}", id, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        Long id = parseId(req.getPathInfo());
        if (id == null) {
            ApiUtils.writeJson(resp, 4001, "id 格式不正确", "null");
            return;
        }

        String checkSql = "SELECT COUNT(*) FROM asset WHERE location_id = ?";
        String deleteSql = "DELETE FROM location_space WHERE id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql);
             PreparedStatement del = conn.prepareStatement(deleteSql)) {
            check.setLong(1, id);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    ApiUtils.writeJson(resp, 4002, "位置空间下存在资产，禁止删除", "null");
                    return;
                }
            }

            del.setLong(1, id);
            int deleted = del.executeUpdate();
            if (deleted == 0) {
                ApiUtils.writeJson(resp, 4004, "位置空间不存在", "null");
                return;
            }

            ApiUtils.writeJson(resp, 0, "ok", "null");
            logger.info("删除位置空间成功 id={}", id);
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("删除位置空间失败 id={}", id, e);
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        String deptIdValue = req.getParameter("deptId");
        Long deptId = parseLongParam(deptIdValue);
        if (deptId == null && !ApiUtils.isBlank(deptIdValue)) {
            ApiUtils.writeJson(resp, 4001, "deptId 格式不正确", "null");
            return;
        }

        int page = parseInt(req.getParameter("page"), 1);
        int size = parseInt(req.getParameter("size"), 10);
        if (page <= 0 || size <= 0) {
            ApiUtils.writeJson(resp, 4001, "page 和 size 需为正整数", "null");
            return;
        }

        boolean hasDept = deptId != null;
        boolean hasKeyword = !ApiUtils.isBlank(keyword);
        List<String> conditions = new ArrayList<>();
        if (hasDept) {
            conditions.add("ls.dept_id = ?");
        }
        if (hasKeyword) {
            conditions.add("ls.room_no LIKE ?");
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String countSql = "SELECT COUNT(*) FROM location_space ls"
                + " JOIN department d ON ls.dept_id = d.id"
                + where;
        String listSql = "SELECT ls.id, ls.dept_id, d.dept_name, ls.room_no, ls.area, ls.remark"
                + " FROM location_space ls"
                + " JOIN department d ON ls.dept_id = d.id"
                + where
                + " ORDER BY ls.id DESC LIMIT ? OFFSET ?";

        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement listPs = conn.prepareStatement(listSql)) {
            String like = hasKeyword ? "%" + keyword + "%" : null;
            int index = 1;
            if (hasDept) {
                countPs.setLong(index, deptId);
                listPs.setLong(index, deptId);
                index++;
            }
            if (hasKeyword) {
                countPs.setString(index, like);
                listPs.setString(index, like);
                index++;
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
                    long deptIdResult = rs.getLong("dept_id");
                    String deptName = rs.getString("dept_name");
                    String roomNo = rs.getString("room_no");
                    BigDecimal area = rs.getBigDecimal("area");
                    String remark = rs.getString("remark");
                    String item = "{ \"id\": " + id
                            + ", \"deptId\": " + deptIdResult
                            + ", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                            + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                            + ", \"area\": " + (area == null ? "0" : area.toPlainString())
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
            logger.error("查询位置空间列表失败", e);
        }
    }

    private void handleDetail(HttpServletResponse resp, long id) throws IOException {
        String sql = "SELECT ls.id, ls.dept_id, d.dept_name, ls.room_no, ls.area, ls.remark"
                + " FROM location_space ls"
                + " JOIN department d ON ls.dept_id = d.id"
                + " WHERE ls.id = ?";
        try (Connection conn = MyDataBase.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    ApiUtils.writeJson(resp, 4004, "位置空间不存在", "null");
                    return;
                }
                long deptId = rs.getLong("dept_id");
                String deptName = rs.getString("dept_name");
                String roomNo = rs.getString("room_no");
                BigDecimal area = rs.getBigDecimal("area");
                String remark = rs.getString("remark");
                String data = "{ \"id\": " + id
                        + ", \"deptId\": " + deptId
                        + ", \"deptName\": \"" + ApiUtils.escapeJson(deptName) + "\""
                        + ", \"roomNo\": \"" + ApiUtils.escapeJson(roomNo) + "\""
                        + ", \"area\": " + (area == null ? "0" : area.toPlainString())
                        + ", \"remark\": " + (remark == null ? "null" : "\"" + ApiUtils.escapeJson(remark) + "\"")
                        + " }";
                ApiUtils.writeJson(resp, 0, "ok", data);
            }
        } catch (SQLException e) {
            ApiUtils.writeJson(resp, 5000, "服务器异常", "null");
            logger.error("查询位置空间详情失败 id={}", id, e);
        }
    }

    private Long parseId(String pathInfo) {
        if (pathInfo == null) {
            return null;
        }
        String value = pathInfo.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return null;
        }
        if (!value.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (ApiUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Long parseLongParam(String value) {
        if (ApiUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractLong(String body, String key) {
        String value = ApiUtils.extractString(body, key, LONG_FIELD);
        if (ApiUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal extractDecimal(String body, String key) {
        String value = ApiUtils.extractString(body, key, DECIMAL_FIELD);
        if (ApiUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
