package com.aiparking.admin.mapper;

import com.aiparking.admin.dto.AdminOrderRow;
import com.aiparking.admin.dto.AdminReservationRow;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AdminQueryMapper {

    @Select("""
            <script>
            SELECT o.id AS id, o.order_no AS orderNo, u.username AS username, l.name AS lotName,
                   o.amount_cents AS amountCents, o.billed_minutes AS billedMinutes,
                   o.status AS status, o.paid_at AS paidAt, o.created_at AS createdAt
            FROM parking_order o
            JOIN `user` u ON u.id = o.user_id
            JOIN parking_lot l ON l.id = o.lot_id
            <where>
                <if test="status != null">AND o.status = #{status}</if>
                <if test="keyword != null and keyword != ''">
                    AND (o.order_no LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
                </if>
            </where>
            ORDER BY o.id DESC
            </script>
            """)
    List<AdminOrderRow> selectOrderPage(IPage<AdminOrderRow> page,
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT r.id AS id, u.username AS username, l.name AS lotName, s.space_no AS spaceNo,
                   r.start_time AS startTime, r.end_time AS endTime, r.status AS status
            FROM reservation r
            JOIN `user` u ON u.id = r.user_id
            JOIN parking_lot l ON l.id = r.lot_id
            JOIN parking_space s ON s.id = r.space_id
            <where>
                <if test="status != null">AND r.status = #{status}</if>
            </where>
            ORDER BY r.start_time DESC
            </script>
            """)
    List<AdminReservationRow> selectReservationPage(IPage<AdminReservationRow> page,
                                                   @Param("status") Integer status);
}
