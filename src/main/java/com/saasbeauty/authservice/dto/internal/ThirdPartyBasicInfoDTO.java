package com.saasbeauty.authservice.dto.internal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyBasicInfoDTO {
    private String firstName;
    private String firstLastName;
}