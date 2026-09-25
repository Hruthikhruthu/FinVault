package com.finvault.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PasswordPolicyTest {
    @ParameterizedTest
    @CsvSource({
        "FinVault#2026,true",
        "Short#1,false",
        "finvault#2026,false",
        "FINVAULT#2026,false",
        "FinVaultOnly,false",
        "FinVault2026,false",
        "'',false"
    })
    void validatesPasswordPolicy(String password, boolean expected) {
        assertThat(PasswordPolicy.isValid(password)).isEqualTo(expected);
        assertThat(PasswordPolicy.message()).contains("uppercase");
    }
}
