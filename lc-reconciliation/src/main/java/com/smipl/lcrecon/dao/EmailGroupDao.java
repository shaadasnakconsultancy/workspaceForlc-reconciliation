package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.EmailGroup;
import com.smipl.lcrecon.model.EmailGroupMember;
import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EmailGroupDao extends BaseDao {

    private static final String GROUP_COLUMNS =
            "id, group_name, description, is_active, created_at, updated_at";

    private static final String MEMBER_COLUMNS =
            "id, group_id, email_address, member_name, is_active, created_at";

    /**
     * Returns all email groups ordered by group_name, with member count.
     */
    public List<EmailGroup> findAll() {
        List<EmailGroup> list = new ArrayList<>();
        String sql = "SELECT g.id, g.group_name, g.description, g.is_active, g.created_at, g.updated_at, " +
                     "(SELECT COUNT(*) FROM email_group_members m WHERE m.group_id = g.id) AS member_count " +
                     "FROM email_groups g ORDER BY g.group_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapGroupRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing email groups", e);
        }
        return list;
    }

    /**
     * Returns a single email group with its members loaded.
     */
    public EmailGroup findById(long id) {
        String sql = "SELECT " + GROUP_COLUMNS + " FROM email_groups WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EmailGroup group = mapGroupRow(rs);
                    group.setMembers(getMembersByGroupId(id));
                    return group;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding email group by id: {}", id, e);
        }
        return null;
    }

    /**
     * Returns active groups for dropdown selection.
     */
    public List<EmailGroup> findActive() {
        List<EmailGroup> list = new ArrayList<>();
        String sql = "SELECT " + GROUP_COLUMNS + " FROM email_groups WHERE is_active = 1 ORDER BY group_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapGroupRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing active email groups", e);
        }
        return list;
    }

    public void create(EmailGroup group) {
        String sql = "INSERT INTO email_groups (group_name, description, is_active) VALUES (?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, group.getGroupName());
            ps.setString(2, group.getDescription());
            ps.setBoolean(3, group.isActive());
            ps.executeUpdate();
            logger.info("Email group created: {}", group.getGroupName());
        } catch (SQLException e) {
            logger.error("Error creating email group: {}", group.getGroupName(), e);
            throw new RuntimeException("Failed to create email group", e);
        }
    }

    public void update(EmailGroup group) {
        String sql = "UPDATE email_groups SET group_name = ?, description = ?, is_active = ?, " +
                     "updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, group.getGroupName());
            ps.setString(2, group.getDescription());
            ps.setBoolean(3, group.isActive());
            ps.setLong(4, group.getId());
            ps.executeUpdate();
            logger.info("Email group updated: {}", group.getGroupName());
        } catch (SQLException e) {
            logger.error("Error updating email group: {}", group.getGroupName(), e);
            throw new RuntimeException("Failed to update email group", e);
        }
    }

    /**
     * Deletes an email group and all its members.
     */
    public void delete(long id) {
        String deleteMembersSql = "DELETE FROM email_group_members WHERE group_id = ?";
        String deleteGroupSql = "DELETE FROM email_groups WHERE id = ?";
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(deleteMembersSql)) {
                ps1.setLong(1, id);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(deleteGroupSql)) {
                ps2.setLong(1, id);
                ps2.executeUpdate();
            }
            conn.commit();
            logger.info("Email group deleted: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting email group: id={}", id, e);
            throw new RuntimeException("Failed to delete email group", e);
        }
    }

    public void addMember(EmailGroupMember member) {
        String sql = "INSERT INTO email_group_members (group_id, email_address, member_name, is_active) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, member.getGroupId());
            ps.setString(2, member.getEmailAddress());
            ps.setString(3, member.getMemberName());
            ps.setBoolean(4, member.isActive());
            ps.executeUpdate();
            logger.info("Member added to group {}: {}", member.getGroupId(), member.getEmailAddress());
        } catch (SQLException e) {
            logger.error("Error adding member to group {}: {}", member.getGroupId(), member.getEmailAddress(), e);
            throw new RuntimeException("Failed to add member", e);
        }
    }

    public void removeMember(long memberId) {
        String sql = "DELETE FROM email_group_members WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            ps.executeUpdate();
            logger.info("Member removed: id={}", memberId);
        } catch (SQLException e) {
            logger.error("Error removing member: id={}", memberId, e);
            throw new RuntimeException("Failed to remove member", e);
        }
    }

    public List<EmailGroupMember> getMembersByGroupId(long groupId) {
        List<EmailGroupMember> list = new ArrayList<>();
        String sql = "SELECT " + MEMBER_COLUMNS + " FROM email_group_members WHERE group_id = ? ORDER BY member_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMemberRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing members for group: {}", groupId, e);
        }
        return list;
    }

    /**
     * Returns comma-separated emails of active members for a group.
     */
    public String getEmailsByGroupId(long groupId) {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT email_address FROM email_group_members WHERE group_id = ? AND is_active = 1 ORDER BY email_address";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    emails.add(rs.getString("email_address"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting emails for group: {}", groupId, e);
        }
        return String.join(",", emails);
    }

    private EmailGroup mapGroupRow(ResultSet rs) throws SQLException {
        EmailGroup group = new EmailGroup();
        group.setId(rs.getLong("id"));
        group.setGroupName(rs.getString("group_name"));
        group.setDescription(rs.getString("description"));
        group.setActive(rs.getBoolean("is_active"));
        group.setCreatedAt(rs.getTimestamp("created_at"));
        group.setUpdatedAt(rs.getTimestamp("updated_at"));
        try { group.setMemberCount(rs.getInt("member_count")); } catch (SQLException ignored) {}
        return group;
    }

    private EmailGroupMember mapMemberRow(ResultSet rs) throws SQLException {
        EmailGroupMember member = new EmailGroupMember();
        member.setId(rs.getLong("id"));
        member.setGroupId(rs.getLong("group_id"));
        member.setEmailAddress(rs.getString("email_address"));
        member.setMemberName(rs.getString("member_name"));
        member.setActive(rs.getBoolean("is_active"));
        member.setCreatedAt(rs.getTimestamp("created_at"));
        return member;
    }
}
