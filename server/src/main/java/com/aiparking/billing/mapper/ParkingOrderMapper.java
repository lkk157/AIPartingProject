package com.aiparking.billing.mapper;

import com.aiparking.billing.entity.ParkingOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ParkingOrderMapper extends BaseMapper<ParkingOrder> {

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
