package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 사용자의 포인트를 조회한다
     */
    public UserPoint getUserPoint(long userId) {
        return new UserPoint(0, 0, 0);
    }

    /**
     * 사용자의 포인트 히스토리를 조회한다
     */
    public List<PointHistory> getUserPointHistory(long userId) {
        return List.of();
    }

    /**
     * 사용자의 포인트를 충전한다
     */
    public UserPoint chargePoint(long userId, long amount) {
        return new UserPoint(0, 0, 0);
    }

    /**
     * 사용자의 포인트를 사용한다
     */
    public UserPoint usePoint(long userId, long amount) {
        return new UserPoint(0, 0, 0);
    }
}
