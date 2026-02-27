package com.larrydevincarter.thufir.tastytrade.controllers;

import com.larrydevincarter.thufir.models.AccountBalance;
import com.larrydevincarter.thufir.tastytrade.clients.TastytradeClient;
import com.larrydevincarter.thufir.models.dtos.TastytradeTokenResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TastytradeController {

    private final TastytradeClient tastytradeClient;

    public TastytradeTokenResponseDto refreshToken(String clientId, String clientSecret, String refreshToken) {
        return tastytradeClient.refreshToken(clientId, clientSecret, refreshToken);
    }

    public AccountBalance getBalances(String accessToken, String accountNumber) {
        return tastytradeClient.getBalances(accessToken, accountNumber);
    }
}
