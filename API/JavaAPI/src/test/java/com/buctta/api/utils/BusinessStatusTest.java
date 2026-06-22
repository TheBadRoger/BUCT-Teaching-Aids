package com.buctta.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessStatusTest {

    @Test
    void success_hasCorrectCode() {
        assertThat(BusinessStatus.SUCCESS.getCode()).isEqualTo(2000);
        assertThat(BusinessStatus.SUCCESS.getTemplate()).isEqualTo("Ok.");
    }

    @Test
    void paramMissing_hasCorrectCode() {
        assertThat(BusinessStatus.PARAM_MISSING.getCode()).isEqualTo(4001);
    }

    @Test
    void accountPasswordError_hasCorrectCode() {
        assertThat(BusinessStatus.ACCOUNT_PASSWORD_ERROR.getCode()).isEqualTo(4011);
    }

    @Test
    void userNotFound_hasCorrectCode() {
        assertThat(BusinessStatus.USER_NOT_FOUND.getCode()).isEqualTo(4041);
    }

    @Test
    void internalError_hasCorrectCode() {
        assertThat(BusinessStatus.INTERNAL_ERROR.getCode()).isEqualTo(5000);
    }

    @Test
    void format_noArgs_returnsTemplate() {
        String result = BusinessStatus.SUCCESS.format();

        assertThat(result).isEqualTo("Ok.");
    }

    @Test
    void format_withArgs_formatsTemplate() {
        String result = BusinessStatus.PARAM_MISSING.format("username");

        assertThat(result).contains("username");
    }

    @Test
    void format_multipleArgs_formatsTemplate() {
        String result = BusinessStatus.IDENTITY_VERIFY_FAILED.format("id mismatch");

        assertThat(result).contains("id mismatch");
    }

    @Test
    void allStatusCodes_areUnique() {
        long uniqueCount = java.util.Arrays.stream(BusinessStatus.values())
                .mapToInt(BusinessStatus::getCode)
                .distinct()
                .count();

        assertThat(uniqueCount).isEqualTo(BusinessStatus.values().length);
    }
}
