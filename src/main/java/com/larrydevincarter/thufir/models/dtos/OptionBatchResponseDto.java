package com.larrydevincarter.thufir.models.dtos;

import com.larrydevincarter.thufir.models.Asset;
import com.larrydevincarter.thufir.models.Option;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
public class OptionBatchResponseDto {
    private List<Asset> assets;
    private Map<String, List<Option>> optionChains;
}