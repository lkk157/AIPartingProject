package com.aiparking.billing.mapper;

import com.aiparking.billing.entity.Reservation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface ReservationMapper extends BaseMapper<Reservation> {

    @Select("""
            SELECT COUNT(1)
            FROM reservation
            WHERE space_id = #{spaceId}
              AND status = 0
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            """)
    long countOverlap(@Param("spaceId") Long spaceId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COUNT(1)
            FROM reservation
            WHERE space_id = #{spaceId}
              AND status = 0
              AND end_time > NOW()
            """)
    long countActiveBySpace(@Param("spaceId") Long spaceId);

    /** 同一车主只能有一条待使用预约：先锁用户行，串行化并发下单 */
    @Select("SELECT id FROM `user` WHERE id = #{userId} FOR UPDATE")
    Long lockUser(@Param("userId") Long userId);

    /**
     * 取该车主当前待使用预约（加锁读，绕过 REPEATABLE READ 快照，确保能看到并发事务已提交的数据）
     */
    @Select("""
            SELECT id
            FROM reservation
            WHERE user_id = #{userId}
              AND status = 0
            LIMIT 1
            FOR UPDATE
            """)
    Long findActiveIdForUpdate(@Param("userId") Long userId);
}
