package com.demo.dao;

import com.demo.config.DatabaseConfig;
import com.demo.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VeranstaltungDaoForKiwi {

    private static final Logger logger = LoggerFactory.getLogger(VeranstaltungDaoForKiwi.class);
    public List<User> getAllVeranstaltung(int start, int size) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, size);
            pstmt.setInt(2, start);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setDepartment(rs.getString("department"));
                    users.add(user);
                }
            }

            logger.debug("Retrieved {} users from database", users.size());
        } catch (SQLException e) {
            logger.error("Error retrieving users", e);
        }
        return users;
    }
}
