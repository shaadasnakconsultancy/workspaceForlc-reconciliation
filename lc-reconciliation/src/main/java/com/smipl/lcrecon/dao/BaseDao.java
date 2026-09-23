package com.smipl.lcrecon.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseDao {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("Error closing Connection", e);
            }
        }
    }

    protected void closeQuietly(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.warn("Error closing PreparedStatement", e);
            }
        }
    }

    protected void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warn("Error closing ResultSet", e);
            }
        }
    }

    /** Trim a value to fit its column, so an over-long message never fails the whole statement. */
    protected String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    protected void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    logger.warn("Error closing resource", e);
                }
            }
        }
    }
}
