package hotel.databaseOperation;

import hotel.classes.UserInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Database operations for the Hotel Management System.
 * Handles all database interactions for user management and bookings.
 *
 * @author Faysal Ahmed
 */
public class DatabaseOperation implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DatabaseOperation.class.getName());
    private final Connection conn;

    /**
     * Creates a new DatabaseOperation instance and establishes a database connection.
     */
    public DatabaseOperation() {
        this.conn = DataBaseConnection.connectTODB();
    }

    /**
     * Closes the database connection.
     */
    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error closing database connection", ex);
        }
    }

    /**
     * Inserts a new customer into the database.
     *
     * @param user The user information to insert
     * @throws SQLException if a database access error occurs
     */
    public void insertCustomer(UserInfo user) throws SQLException {
        String insertQuery = "INSERT INTO userInfo (name, address, phone, type) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getAddress());
            stmt.setString(3, user.getPhoneNo());
            stmt.setString(4, user.getType());
            
            stmt.executeUpdate();
            LOGGER.info("Successfully inserted new customer: " + user.getName());
            JOptionPane.showMessageDialog(null, "Successfully inserted new customer");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to insert customer", ex);
            throw new SQLException("Failed to insert customer: " + ex.getMessage(), ex);
        }
    }

    /**
     * Updates an existing customer's information.
     *
     * @param user The user information to update
     * @throws SQLException if a database access error occurs
     */
    public void updateCustomer(UserInfo user) throws SQLException {
        String updateQuery = "UPDATE userInfo SET name = ?, address = ?, phone = ?, type = ? WHERE user_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getAddress());
            stmt.setString(3, user.getPhoneNo());
            stmt.setString(4, user.getType());
            stmt.setInt(5, user.getCustomerId());
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                LOGGER.info("Successfully updated customer: " + user.getName());
                JOptionPane.showMessageDialog(null, "Successfully updated customer");
            } else {
                throw new SQLException("No customer found with ID: " + user.getCustomerId());
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update customer", ex);
            throw new SQLException("Failed to update customer: " + ex.getMessage(), ex);
        }
    }

    /**
     * Deletes a customer from the database.
     *
     * @param userId The ID of the user to delete
     * @throws SQLException if a database access error occurs
     */
    public void deleteCustomer(int userId) throws SQLException {
        String deleteQuery = "DELETE FROM userInfo WHERE user_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
            stmt.setInt(1, userId);
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                LOGGER.info("Successfully deleted customer with ID: " + userId);
                JOptionPane.showMessageDialog(null, "Successfully deleted user");
            } else {
                throw new SQLException("No customer found with ID: " + userId);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to delete customer", ex);
            throw new SQLException("Failed to delete customer: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets all customers from the database.
     *
     * @return ResultSet containing all customers
     * @throws SQLException if a database access error occurs
     */
    public ResultSet getAllCustomers() throws SQLException {
        String query = "SELECT * FROM userInfo";
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            return stmt.executeQuery();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to get all customers", ex);
            throw new SQLException("Failed to get all customers: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets available rooms for a given check-in time.
     *
     * @param checkInTime The check-in time to check availability for
     * @return ResultSet containing available rooms
     * @throws SQLException if a database access error occurs
     */
    public ResultSet getAvailableRooms(long checkInTime) throws SQLException {
        String query = "SELECT room_no FROM room " +
                      "LEFT OUTER JOIN booking ON room.room_no = booking.booking_room " +
                      "WHERE booking.booking_room IS NULL " +
                      "OR ? < booking.check_in " +
                      "OR booking.check_out < ? " +
                      "GROUP BY room.room_no ORDER BY room_no";
        
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, checkInTime);
            stmt.setLong(2, checkInTime);
            return stmt.executeQuery();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to get available rooms", ex);
            throw new SQLException("Failed to get available rooms: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets booking information for a room within a date range.
     *
     * @param startDate Start date for the booking search
     * @param endDate End date for the booking search
     * @param roomNo Room number to check
     * @return ResultSet containing booking information
     * @throws SQLException if a database access error occurs
     */
    public ResultSet getBookingInfo(long startDate, long endDate, String roomNo) throws SQLException {
        String query = "SELECT * FROM booking WHERE booking_room = ? AND (" +
                      "(check_in <= ? AND (check_out = 0 OR check_out <= ?)) OR " +
                      "(check_in > ? AND check_out < ?) OR " +
                      "(check_in <= ? AND (check_out = 0 OR check_out > ?)))";
        
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, roomNo);
            stmt.setLong(2, startDate);
            stmt.setLong(3, endDate);
            stmt.setLong(4, startDate);
            stmt.setLong(5, endDate);
            stmt.setLong(6, endDate);
            stmt.setLong(7, endDate);
            
            return stmt.executeQuery();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to get booking information", ex);
            throw new SQLException("Failed to get booking information: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets a customer's ID based on their name and phone number.
     *
     * @param user The user information to search with
     * @return The user's ID, or -1 if not found
     * @throws SQLException if a database access error occurs
     */
    public int getCustomerId(UserInfo user) throws SQLException {
        String query = "SELECT user_id FROM userInfo WHERE name = ? AND phone = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPhoneNo());
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to get customer ID", ex);
            throw new SQLException("Failed to get customer ID: " + ex.getMessage(), ex);
        }
    }

    /**
     * Searches for users by name pattern.
     *
     * @param namePattern The pattern to search for in user names
     * @return ResultSet containing matching users
     * @throws SQLException if a database access error occurs
     */
    public ResultSet searchUsers(String namePattern) throws SQLException {
        String query = "SELECT user_id, name, address FROM userInfo WHERE name LIKE ?";
        
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + namePattern + "%");
            return stmt.executeQuery();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to search users", ex);
            throw new SQLException("Failed to search users: " + ex.getMessage(), ex);
        }
    }

    public void flushAll() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error closing database connection", ex);
        }
    }

    public ResultSet searchAnUser(int userId) throws SQLException {
        String query = "SELECT * FROM userInfo WHERE user_id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            return stmt.executeQuery();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to search user", ex);
            throw new SQLException("Failed to search user: " + ex.getMessage(), ex);
        }
    }

    public ResultSet searchUser(String namePattern) throws SQLException {
        String query = "SELECT * FROM userInfo WHERE name LIKE ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + namePattern + "%");
            return stmt.executeQuery();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to search users", ex);
            throw new SQLException("Failed to search users: " + ex.getMessage(), ex);
        }
    }
}
