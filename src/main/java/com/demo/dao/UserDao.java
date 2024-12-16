package com.demo.dao;

import com.demo.config.DatabaseConfig;
import com.demo.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    public void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "email VARCHAR(255), " +
                "department VARCHAR(255))";

        // Gerçekçi isimler ve soyisimler
        String[] firstNames = {"John", "Emma", "Michael", "Sarah", "David", "Lisa", "James", "Emily",
                "Robert", "Maria", "William", "Anna", "Thomas", "Linda", "Daniel", "Patricia",
                "Joseph", "Elizabeth", "Charles", "Jennifer"};

        String[] lastNames = {"Smith", "Johnson", "Brown", "Davis", "Wilson", "Anderson", "Taylor", "Thomas",
                "Moore", "Martin", "Thompson", "White", "Harris", "Miller", "Clark", "Walker",
                "Hall", "Young", "Allen", "King"};

        String[] departments = {"Information Technology", "Human Resources", "Finance", "Marketing",
                "Sales", "Engineering", "Customer Support", "Research & Development",
                "Operations", "Legal"};

        StringBuilder insertDataSQL = new StringBuilder("INSERT INTO users (name, email, department) VALUES ");
        Random random = new Random();

        // Let's create 200 realistic users
        for (int i = 1; i <= 2000; i++) {
            if (i > 1) {
                insertDataSQL.append(", ");
            }

            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String fullName = firstName + " " + lastName;

            // Email oluştur (lowercase ve nokta ile)
            String email = (firstName + "." + lastName + "@company.com").toLowerCase();

            String dept = departments[random.nextInt(departments.length)];

            insertDataSQL.append(String.format("('%s', '%s', '%s')",
                    fullName, email, dept));
        }

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);

            // Check if table is empty before inserting sample data
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.execute(insertDataSQL.toString());
                logger.info("Realistic sample data inserted successfully");
            }

        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public List<User> getAllUsers(int start, int size) {
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

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                logger.debug("Total user count: {}", count);
                return count;
            }
        } catch (SQLException e) {
            logger.error("Error getting total count", e);
        }
        return 0;
    }

    public User getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setDepartment(rs.getString("department"));
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving user with id: {}", id, e);
        }
        return null;
    }

    public void saveUser(User user) {
        String sql = "INSERT INTO users (name, email, department) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getDepartment());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getLong(1));
                        logger.info("User saved successfully with id: {}", user.getId());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving user", e);
        }
    }

    public void updateUser(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, department = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getDepartment());
            pstmt.setLong(4, user.getId());

            int affectedRows = pstmt.executeUpdate();
            logger.info("Updated {} rows for user id: {}", affectedRows, user.getId());

        } catch (SQLException e) {
            logger.error("Error updating user with id: {}", user.getId(), e);
        }
    }

    public void deleteUser(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();
            logger.info("Deleted {} rows for user id: {}", affectedRows, id);

        } catch (SQLException e) {
            logger.error("Error deleting user with id: {}", id, e);
        }
    }

    public void deleteAllUsers() {
        String sql = "DELETE FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            int affectedRows = stmt.executeUpdate(sql);
            logger.info("Deleted all users. Affected rows: {}", affectedRows);

        } catch (SQLException e) {
            logger.error("Error deleting all users", e);
        }
    }
}