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
}
