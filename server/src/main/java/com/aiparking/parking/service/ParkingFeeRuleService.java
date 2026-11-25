package com.aiparking.parking.service;

import com.aiparking.common.NotFoundException;
import com.aiparking.parking.dto.FeeRuleRequest;
import com.aiparking.parking.dto.FeeRuleVO;
import com.aiparking.parking.entity.ParkingFeeRule;
import com.aiparking.parking.mapper.ParkingFeeRuleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParkingFeeRuleService {

    private final ParkingFeeRuleMapper feeRuleMapper;
    private final ParkingLotService parkingLotService;

    public ParkingFeeRuleService(ParkingFeeRuleMapper feeRuleMapper, ParkingLotService parkingLotService) {
        this.feeRuleMapper = feeRuleMapper;
        this.parkingLotService = parkingLotService;
    }

    /**
     * 新建规则即启用：同一停车场只保留一条启用规则。
     */
    @Transactional
    public FeeRuleVO create(Long lotId, FeeRuleRequest request) {
        parkingLotService.requireLot(lotId);

        ParkingFeeRule disable = new ParkingFeeRule();
        disable.setEnabled(0);
        feeRuleMapper.update(disable, Wrappers.<ParkingFeeRule>lambdaUpdate()
                .eq(ParkingFeeRule::getLotId, lotId)
                .eq(ParkingFeeRule::getEnabled, 1));

        ParkingFeeRule rule = new ParkingFeeRule();
        rule.setLotId(lotId);
        apply(rule, request);
        rule.setEnabled(1);
        feeRuleMapper.insert(rule);
        return toVO(rule);
    }

    public List<FeeRuleVO> list(Long lotId) {
        parkingLotService.requireLot(lotId);
        return feeRuleMapper.selectList(Wrappers.<ParkingFeeRule>lambdaQuery()
                        .eq(ParkingFeeRule::getLotId, lotId)
                        .orderByDesc(ParkingFeeRule::getId))
                .stream().map(ParkingFeeRuleService::toVO).toList();
    }

    public FeeRuleVO update(Long ruleId, FeeRuleRequest request) {
        ParkingFeeRule rule = requireRule(ruleId);
        apply(rule, request);
        feeRuleMapper.updateById(rule);
        return toVO(rule);
    }

    public void delete(Long ruleId) {
        requireRule(ruleId);
        feeRuleMapper.deleteById(ruleId);
    }

    public ParkingFeeRule requireEnabledRule(Long lotId) {
        ParkingFeeRule rule = findEnabledRule(lotId);
        if (rule == null) {
            throw new NotFoundException("该停车场未配置收费规则");
        }
        return rule;
    }

    /** 未配置规则时返回 null（供"预估费用"等非强制场景使用） */
    public ParkingFeeRule findEnabledRule(Long lotId) {
        return feeRuleMapper.selectOne(Wrappers.<ParkingFeeRule>lambdaQuery()
                .eq(ParkingFeeRule::getLotId, lotId)
                .eq(ParkingFeeRule::getEnabled, 1)
                .orderByDesc(ParkingFeeRule::getId)
                .last("LIMIT 1"));
    }

    private ParkingFeeRule requireRule(Long ruleId) {
        ParkingFeeRule rule = feeRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new NotFoundException("收费规则不存在");
        }
        return rule;
    }

    private void apply(ParkingFeeRule rule, FeeRuleRequest request) {
        rule.setRuleName(request.getRuleName());
        rule.setFreeMinutes(request.getFreeMinutes());
        rule.setFirstHourCents(request.getFirstHourCents());
        rule.setExtraPerHourCents(request.getExtraPerHourCents());
        rule.setDailyCapCents(request.getDailyCapCents());
    }

    static FeeRuleVO toVO(ParkingFeeRule rule) {
        return new FeeRuleVO(rule.getId(), rule.getLotId(), rule.getRuleName(), rule.getFreeMinutes(),
                rule.getFirstHourCents(), rule.getExtraPerHourCents(), rule.getDailyCapCents(), rule.getEnabled());
    }
}
