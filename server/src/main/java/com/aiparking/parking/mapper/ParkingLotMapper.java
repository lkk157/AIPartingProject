package com.aiparking.parking.mapper;

import com.aiparking.parking.entity.ParkingLot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ParkingLotMapper extends BaseMapper<ParkingLot> {

    @Insert("INSERT INTO parking_lot_admin(lot_id, user_id) VALUES(#{lotId}, #{userId})")
    int bindAdmin(@Param("lotId") Long lotId, @Param("userId") Long userId);

    @Delete("DELETE FROM parking_lot_admin WHERE lot_id = #{lotId} AND user_id = #{userId}")
    int unbindAdmin(@Param("lotId") Long lotId, @Param("userId") Long userId);

    @Select("SELECT user_id FROM parking_lot_admin WHERE lot_id = #{lotId}")
    List<Long> selectAdminIds(@Param("lotId") Long lotId);
}
