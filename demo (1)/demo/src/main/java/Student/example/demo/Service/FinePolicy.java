package Student.example.demo.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FinePolicy {
    public static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Colombo");

    private final int loanPeriodDays;
    private final BigDecimal dailyRate;
    private final BigDecimal borrowingCharge;

    public FinePolicy(
            @Value("${library.loan-period-days:3}") int loanPeriodDays,
            @Value("${library.daily-late-fine-rate:10.00}") BigDecimal dailyRate,
            @Value("${library.borrowing-charge:50.00}") BigDecimal borrowingCharge) {
        this.loanPeriodDays = loanPeriodDays;
        this.dailyRate = dailyRate;
        this.borrowingCharge = borrowingCharge;
    }

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public BigDecimal getBorrowingCharge() {
        return borrowingCharge;
    }
}
