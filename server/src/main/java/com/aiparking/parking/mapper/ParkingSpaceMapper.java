package com.aiparking.parking.mapper;

import com.aiparking.parking.entity.ParkingSpace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ParkingSpaceMapper extends BaseMapper<ParkingSpace> {

    /**
     * 条件更新：只有当前状态仍为 fromStatus 时才更新，避免并发覆盖。
     */
    @Update("""
            UPDATE parking_space
            SET status = #{toStatus}, updated_at = NOW()
            WHERE id = #{spaceId} AND status = #{fromStatus}
            """)
    int updateStatusIfMatch(@Param("spaceId") Long spaceId,
                            @Param("fromStatus") int fromStatus,
                            @Param("toStatus") int toStatus);

    @Select("SELECT COUNT(1) FROM parking_space WHERE lot_id = #{lotId} AND status = #{status}")
    long countByLotAndStatus(@Param("lotId") Long lotId, @Param("status") int status);

    @Select("SELECT COUNT(1) FROM parking_space WHERE lot_id = #{lotId}")
    long countByLot(@Param("lotId") Long lotId);
}
