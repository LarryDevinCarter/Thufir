package com.larrydevincarter.thufir.services;

import java.math.BigDecimal;
import java.util.Map;

public interface TastytradeService {

    void getAccessToken();

    void forceRefresh();

    Map<String, Object> getCurrentBalances();

    BigDecimal getAvailableCash();
}
