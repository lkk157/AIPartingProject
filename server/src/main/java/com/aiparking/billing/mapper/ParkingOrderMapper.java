package com.aiparking.billing.mapper;

import com.aiparking.billing.entity.ParkingOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ParkingOrderMapper extends BaseMapper<ParkingOrder> {

    @Select("""
            SELECT COALESCE(SUM(amount_cents), 0)
            FROM parking_order
            WHERE status = 1 AND paid_at >= #{from}
            """)
    long sumPaidAmountSince(@Param("from") LocalDateTime from);

    @Update("""
            UPDATE parking_order
            SET status = #{toStatus}, paid_at = #{paidAt}, updated_at = NOW()
            WHERE id = #{orderId} AND status = #{fromStatus}
            """)
    int updateStatusIfMatch(@Param("orderId") Long orderId,
                            @Param("fromStatus") int fromStatus,
                            @Param("toStatus") int toStatus,
                            @Param("paidAt") LocalDateTime paidAt);
}
