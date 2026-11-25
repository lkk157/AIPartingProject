package com.aiparking.parking.enums;

import com.aiparking.common.BusinessException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 车位状态机：只允许显式声明的流转，其余一律拒绝。
 */
public final class SpaceStateMachine {

    private static final Map<SpaceStatus, Set<SpaceStatus>> ALLOWED = new EnumMap<>(SpaceStatus.class);

    static {
        ALLOWED.put(SpaceStatus.FREE, EnumSet.of(SpaceStatus.OCCUPIED, SpaceStatus.RESERVED, SpaceStatus.MAINTENANCE));
        ALLOWED.put(SpaceStatus.OCCUPIED, EnumSet.of(SpaceStatus.FREE));
        ALLOWED.put(SpaceStatus.RESERVED, EnumSet.of(SpaceStatus.OCCUPIED, SpaceStatus.FREE));
        ALLOWED.put(SpaceStatus.MAINTENANCE, EnumSet.of(SpaceStatus.FREE));
    }

    private SpaceStateMachine() {
    }

    public static boolean canTransit(SpaceStatus from, SpaceStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void validate(SpaceStatus from, SpaceStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(
                    "车位状态不允许从「" + from.getText() + "」变更为「" + to.getText() + "」");
        }
    }
}
