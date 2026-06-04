package com.bankie.bankie_api.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SetDailyLimitRequestDTO {
    private BigDecimal dailyTransferLimit;
}