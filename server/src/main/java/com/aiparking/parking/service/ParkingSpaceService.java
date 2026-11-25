package com.aiparking.parking.service;

import com.aiparking.common.BusinessException;
import com.aiparking.common.NotFoundException;
import com.aiparking.common.PageResult;
import com.aiparking.parking.dto.BatchSpaceRequest;
import com.aiparking.parking.dto.SpaceStatusChangeRequest;
import com.aiparking.parking.dto.SpaceVO;
import com.aiparking.parking.entity.ParkingLot;
import com.aiparking.parking.entity.ParkingSpace;
import com.aiparking.parking.entity.ParkingSpaceStatusLog;
import com.aiparking.parking.enums.SpaceStateMachine;
import com.aiparking.parking.enums.SpaceStatus;
import com.aiparking.parking.mapper.ParkingLotMapper;
import com.aiparking.parking.mapper.ParkingSpaceMapper;
import com.aiparking.parking.mapper.ParkingSpaceStatusLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingSpaceService {

    private final ParkingSpaceMapper spaceMapper;
    private final ParkingSpaceStatusLogMapper statusLogMapper;
    private final ParkingLotMapper lotMapper;
    private final ParkingLotService parkingLotService;

    public ParkingSpaceService(ParkingSpaceMapper spaceMapper,
                               ParkingSpaceStatusLogMapper statusLogMapper,
                               ParkingLotMapper lotMapper,
                               ParkingLotService parkingLotService) {
        this.spaceMapper = spaceMapper;
        this.statusLogMapper = statusLogMapper;
        this.lotMapper = lotMapper;
        this.parkingLotService = parkingLotService;
    }

    @Transactional
    public int batchCreate(Long lotId, BatchSpaceRequest request) {
        parkingLotService.requireLot(lotId);

        List<String> spaceNos = new ArrayList<>();
        for (int i = 1; i <= request.getCount(); i++) {
            spaceNos.add(String.format("%s-%03d", request.getPrefix(), i));
        }

        Long duplicated = spaceMapper.selectCount(Wrappers.<ParkingSpace>lambdaQuery()
                .eq(ParkingSpace::getLotId, lotId)
                .in(ParkingSpace::getSpaceNo, spaceNos));
        if (duplicated != null && duplicated > 0) {
            List<String> existing = spaceMapper.selectList(Wrappers.<ParkingSpace>lambdaQuery()
                            .eq(ParkingSpace::getLotId, lotId)
                            .in(ParkingSpace::getSpaceNo, spaceNos))
                    .stream().map(ParkingSpace::getSpaceNo).sorted().toList();
            throw new BusinessException("车位编号已存在：" + String.join("、", existing));
        }

        String type = StringUtils.hasText(request.getType()) ? request.getType() : "NORMAL";
        for (String no : spaceNos) {
            ParkingSpace space = new ParkingSpace();
            space.setLotId(lotId);
            space.setSpaceNo(no);
            space.setZone(request.getZone());
            space.setType(type);
            space.setStatus(SpaceStatus.FREE.getCode());
            spaceMapper.insert(space);
        }

        ParkingLot update = new ParkingLot();
        update.setId(lotId);
        update.setCapacity((int) spaceMapper.countByLot(lotId));
        lotMapper.updateById(update);
        return spaceNos.size();
    }

    public PageResult<SpaceVO> page(Long lotId, long page, long size, Integer status) {
        parkingLotService.requireLot(lotId);
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 200);

        LambdaQueryWrapper<ParkingSpace> query = Wrappers.<ParkingSpace>lambdaQuery()
                .eq(ParkingSpace::getLotId, lotId);
        if (status != null) {
            query.eq(ParkingSpace::getStatus, status);
        }
        query.orderByAsc(ParkingSpace::getSpaceNo);

        Page<ParkingSpace> result = spaceMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<SpaceVO> records = result.getRecords().stream().map(ParkingSpaceService::toVO).toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Transactional
    public SpaceVO changeStatus(Long spaceId, SpaceStatusChangeRequest request, Long operatorId) {
        ParkingSpace space = spaceMapper.selectById(spaceId);
        if (space == null) {
            throw new NotFoundException("车位不存在");
        }

        SpaceStatus from = SpaceStatus.fromCode(space.getStatus());
        SpaceStatus to = SpaceStatus.fromCode(request.getStatus());
        SpaceStateMachine.validate(from, to);

        int updated = spaceMapper.updateStatusIfMatch(spaceId, from.getCode(), to.getCode());
        if (updated == 0) {
            throw new BusinessException("车位状态已被其他操作变更，请刷新后重试");
        }

        ParkingSpaceStatusLog log = new ParkingSpaceStatusLog();
        log.setSpaceId(spaceId);
        log.setFromStatus(from.getCode());
        log.setToStatus(to.getCode());
        log.setReason(request.getReason());
        log.setOperatorId(operatorId);
        statusLogMapper.insert(log);

        space.setStatus(to.getCode());
        return toVO(space);
    }

    public List<SpaceVO> listByLot(Long lotId, Integer status) {
        parkingLotService.requireLot(lotId);
        LambdaQueryWrapper<ParkingSpace> query = Wrappers.<ParkingSpace>lambdaQuery()
                .eq(ParkingSpace::getLotId, lotId);
        if (status != null) {
            query.eq(ParkingSpace::getStatus, status);
        }
        query.orderByAsc(ParkingSpace::getSpaceNo);
        return spaceMapper.selectList(query).stream()
                .map(ParkingSpaceService::toVO)
                .collect(Collectors.toList());
    }

    static SpaceVO toVO(ParkingSpace space) {
        SpaceStatus status = SpaceStatus.fromCode(space.getStatus());
        return new SpaceVO(space.getId(), space.getLotId(), space.getSpaceNo(), space.getZone(),
                space.getType(), status.getCode(), status.getText());
    }
}
