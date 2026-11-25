package com.aiparking.billing.mapper;

import com.aiparking.billing.entity.ParkingRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ParkingRecordMapper extends BaseMapper<ParkingRecord> {

    @Update("""
            UPDATE parking_record
            SET status = 1, exit_time = #{exitTime}, updated_at = NOW()
            WHERE id = #{recordId} AND status = 0
            """)
    int markExited(@Param("recordId") Long recordId, @Param("exitTime") LocalDateTime exitTime);
}
