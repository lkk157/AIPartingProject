package com.aiparking.parking.service;

import com.aiparking.common.BusinessException;
import com.aiparking.common.NotFoundException;
import com.aiparking.common.PageResult;
import com.aiparking.parking.dto.FeeRuleVO;
import com.aiparking.parking.dto.LotDetailVO;
import com.aiparking.parking.dto.LotRequest;
import com.aiparking.parking.dto.LotVO;
import com.aiparking.parking.entity.ParkingFeeRule;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.enums.SpaceStatus;
import com.aiparking.parking.mapper.ParkingFeeRuleMapper;
import com.aiparking.parking.mapper.ParkingLotMapper;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ParkingLotService {

    private final ParkingLotMapper lotMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final ParkingFeeRuleMapper feeRuleMapper;
    private final UserMapper userMapper;

    public ParkingLotService(ParkingLotMapper lotMapper, ParkingSpaceMapper spaceMapper,
                             ParkingFeeRuleMapper feeRuleMapper, UserMapper userMapper) {
        this.lotMapper = lotMapper;
        this.spaceMapper = spaceMapper;
        this.feeRuleMapper = feeRuleMapper;
        this.userMapper = userMapper;
    }

    public PageResult<LotVO> page(long page, long size, String keyword, String area, Integer status) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);

        LambdaQueryWrapper<ParkingLot> query = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(ParkingLot::getName, keyword)
                    .or().like(ParkingLot::getAddress, keyword));
        }
        if (StringUtils.hasText(area)) {
            query.eq(ParkingLot::getArea, area);
        }
        if (status != null) {
            query.eq(ParkingLot::getStatus, status);
        }
        query.orderByDesc(ParkingLot::getId);

        Page<ParkingLot> result = lotMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<LotVO> records = result.getRecords().stream().map(ParkingLotService::toVO).toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    public LotVO create(LotRequest request) {
        ParkingLot lot = new ParkingLot();
        apply(lot, request);
        lot.setCapacity(request.getCapacity() == null ? 0 : request.getCapacity());
        lot.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        lotMapper.insert(lot);
        return toVO(lot);
    }

    public LotVO update(Long id, LotRequest request) {
        ParkingLot lot = requireLot(id);
        apply(lot, request);
        if (request.getCapacity() != null) {
            lot.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null) {
            lot.setStatus(request.getStatus());
        }
        lotMapper.updateById(lot);
        return toVO(lot);
    }

    public void delete(Long id) {
        requireLot(id);
        lotMapper.deleteById(id);
    }

    public LotDetailVO detail(Long id) {
        ParkingLot lot = requireLot(id);
        ParkingFeeRule enabledRule = feeRuleMapper.selectOne(Wrappers.<ParkingFeeRule>lambdaQuery()
                .eq(ParkingFeeRule::getLotId, id)
                .eq(ParkingFeeRule::getEnabled, 1)
                .orderByDesc(ParkingFeeRule::getId)
                .last("LIMIT 1"));

        return new LotDetailVO(
                toVO(lot),
                spaceMapper.countByLot(id),
                spaceMapper.countByLotAndStatus(id, SpaceStatus.FREE.getCode()),
                spaceMapper.countByLotAndStatus(id, SpaceStatus.OCCUPIED.getCode()),
                spaceMapper.countByLotAndStatus(id, SpaceStatus.RESERVED.getCode()),
                spaceMapper.countByLotAndStatus(id, SpaceStatus.MAINTENANCE.getCode()),
                enabledRule == null ? null : new FeeRuleVO(enabledRule.getId(), enabledRule.getLotId(),
                        enabledRule.getRuleName(), enabledRule.getFreeMinutes(), enabledRule.getFirstHourCents(),
                        enabledRule.getExtraPerHourCents(), enabledRule.getDailyCapCents(), enabledRule.getEnabled()),
                lotMapper.selectAdminIds(id));
    }

    public void bindAdmin(Long lotId, Long userId) {
        requireLot(lotId);
        if (userMapper.selectById(userId) == null) {
            throw new NotFoundException("用户不存在");
        }
        if (lotMapper.selectAdminIds(lotId).contains(userId)) {
            throw new BusinessException("该用户已绑定此停车场");
        }
        lotMapper.bindAdmin(lotId, userId);
    }

    public void unbindAdmin(Long lotId, Long userId) {
        requireLot(lotId);
        if (lotMapper.unbindAdmin(lotId, userId) == 0) {
            throw new NotFoundException("该用户未绑定此停车场");
        }
    }

    public ParkingLot requireLot(Long id) {
        ParkingLot lot = lotMapper.selectById(id);
        if (lot == null) {
            throw new NotFoundException("停车场不存在");
        }
        return lot;
    }

    private void apply(ParkingLot lot, LotRequest request) {
        lot.setName(request.getName());
        lot.setAddress(request.getAddress());
        lot.setArea(request.getArea());
        lot.setDescription(request.getDescription());
    }

    static LotVO toVO(ParkingLot lot) {
        return new LotVO(lot.getId(), lot.getName(), lot.getAddress(), lot.getArea(),
                lot.getCapacity(), lot.getStatus(), lot.getDescription(), lot.getCreatedAt());
    }
}
