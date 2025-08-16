package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private PointService pointService;
    
    @Mock
    private UserPointTable userPointTable;
    
    @Mock
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("사용자의 포인트를 조회할 수 있다")
    void shouldReturnUserPoint() {
        // given
        long userId = 1L;
        UserPoint expectedUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);
        
        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
        assertThat(result.updateMillis()).isPositive();
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("사용자의 포인트 히스토리를 조회할 수 있다")
    void shouldReturnPointHistories() {
        // given
        long userId = 1L;
        List<PointHistory> expectedHistories = List.of(
            new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
            new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistories);
        
        // when
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedHistories);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("사용자의 포인트를 충전할 수 있다")
    void shouldChargeUserPoint() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint currentUserPoint = new UserPoint(userId, 500L, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId)).thenReturn(currentUserPoint);
        when(userPointTable.insertOrUpdate(userId, 1500L)).thenReturn(updatedUserPoint);
        when(pointHistoryTable.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
            .thenReturn(new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis()));
        
        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1500L);
        assertThat(result.updateMillis()).isPositive();
        
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 1500L);
        verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("사용자의 포인트를 사용할 수 있다")
    void shouldUseUserPoint() {
        // given
        long userId = 1L;
        long useAmount = 500L;
        UserPoint currentUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 500L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId)).thenReturn(currentUserPoint);
        when(userPointTable.insertOrUpdate(userId, 500L)).thenReturn(updatedUserPoint);
        when(pointHistoryTable.insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong()))
            .thenReturn(new PointHistory(1L, userId, useAmount, TransactionType.USE, System.currentTimeMillis()));
        
        // when
        UserPoint result = pointService.usePoint(userId, useAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(500L);
        assertThat(result.updateMillis()).isPositive();
        
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 500L);
        verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
    }

    // 예외 상황 테스트
    @Test
    @DisplayName("음수 금액으로 포인트 충전 시 예외가 발생한다")
    void shouldThrowExceptionWhenChargeWithNegativeAmount() {
        // given
        long userId = 1L;
        long negativeAmount = -1000L;
        
        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, negativeAmount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("충전/사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0원으로 포인트 충전 시 예외가 발생한다")
    void shouldThrowExceptionWhenChargeWithZeroAmount() {
        // given
        long userId = 1L;
        long zeroAmount = 0L;
        
        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, zeroAmount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("충전/사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("보유 포인트보다 많은 금액을 사용할 때 예외가 발생한다")
    void shouldThrowExceptionWhenUseMoreThanBalance() {
        // given
        long userId = 1L;
        long useAmount = 1000L;
        UserPoint currentPoint = new UserPoint(userId, 500L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId)).thenReturn(currentPoint);
        
        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("보유 포인트가 부족합니다. 현재: 500, 필요: 1000");
    }

}
