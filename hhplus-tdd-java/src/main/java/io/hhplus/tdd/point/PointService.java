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
        return userPointTable.selectById(userId);
    }

    /**
     * 사용자의 포인트 히스토리를 조회한다
     */
    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 사용자의 포인트를 충전한다
     */
    public UserPoint chargePoint(long userId, long amount) {
        validatePositiveAmount(amount);
        
        UserPoint currentPoint = userPointTable.selectById(userId);
        long newPoint = currentPoint.point() + amount;
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
        
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        
        return updatedPoint;
    }

    /**
     * 사용자의 포인트를 사용한다
     */
    public UserPoint usePoint(long userId, long amount) {
        validatePositiveAmount(amount);
        
        UserPoint currentPoint = userPointTable.selectById(userId);
        validateSufficientBalance(currentPoint.point(), amount);
        
        long newPoint = currentPoint.point() - amount;
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPoint);
        
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        
        return updatedPoint;
    }

    /**
     * 금액이 양수인지 검증
     */
    private void validatePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전/사용 금액은 0보다 커야 합니다");
        }
    }

    /**
     * 잔액이 충분한지 검증
     */
    private void validateSufficientBalance(long currentBalance, long useAmount) {
        if (currentBalance < useAmount) {
            throw new IllegalArgumentException("보유 포인트가 부족합니다. 현재: " + currentBalance + ", 필요: " + useAmount);
        }
    }
}
