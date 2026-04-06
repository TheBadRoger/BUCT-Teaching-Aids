package com.buctta.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_noData_returnsSuccessResponse() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.getCode()).isEqualTo(BusinessStatus.SUCCESS.getCode());
        assertThat(response.getMsg()).isEqualTo(BusinessStatus.SUCCESS.getTemplate());
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isPositive();
    }

    @Test
    void ok_withData_returnsSuccessResponseWithData() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.ok(data);

        assertThat(response.getCode()).isEqualTo(BusinessStatus.SUCCESS.getCode());
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void fail_withCodeAndMsg_returnsFailureResponse() {
        ApiResponse<Void> response = ApiResponse.fail(4001, "error message");

        assertThat(response.getCode()).isEqualTo(4001);
        assertThat(response.getMsg()).isEqualTo("error message");
        assertThat(response.getData()).isNull();
    }

    @Test
    void fail_withBusinessStatus_returnsFailureResponse() {
        ApiResponse<Void> response = ApiResponse.fail(BusinessStatus.USER_NOT_FOUND);

        assertThat(response.getCode()).isEqualTo(BusinessStatus.USER_NOT_FOUND.getCode());
        assertThat(response.getMsg()).isEqualTo(BusinessStatus.USER_NOT_FOUND.getTemplate());
    }

    @Test
    void fail_withBusinessStatusAndArgs_returnsFormattedMessage() {
        ApiResponse<Void> response = ApiResponse.fail(BusinessStatus.PARAM_MISSING, "username");

        assertThat(response.getCode()).isEqualTo(BusinessStatus.PARAM_MISSING.getCode());
        assertThat(response.getMsg()).contains("username");
    }

    @Test
    void constructor_allArgs_setsAllFields() {
        ApiResponse<String> response = new ApiResponse<>(200, "ok", 12345L, "data");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMsg()).isEqualTo("ok");
        assertThat(response.getTimestamp()).isEqualTo(12345L);
        assertThat(response.getData()).isEqualTo("data");
    }
}
