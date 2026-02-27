package com.larrydevincarter.thufir.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.larrydevincarter.thufir.models.AccountBalance;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record AccountBalanceResponseDto(

        @JsonProperty("data")
        AccountBalance data

) {}