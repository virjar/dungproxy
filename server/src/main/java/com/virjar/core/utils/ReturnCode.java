package com.virjar.core.utils;

public enum ReturnCode {
    EXCEPTION("-1", ""), SUCCEED("0", "succeed"), MOBILE_FORMAT_INVALID("1", "mobile.format.invalid"), EMAIL_FORMAT_INVALID(
            "2", "email.format.invalid"), PASSWORD_FORMAT_INVALID("3", "pwd.format.invalid"), ACCOUNT_ALREADY_EXIST(
            "4", "account.already.exist"), SEND_SMS_FAILED("5", "send.sms.failed"), REGISTER_PARAM_EMPTY("6",
            "register.param.empty"), TOKEN_FORMAT_INVALID("7", "token.format.invalid"), TOKEN_NOT_EXIST("8",
            "token.not.exist"), TOKEN_NOT_CORRECT("9", "token.not.correct"), TOKEN_OUT_DATED("10", "token.out.dated"), ACCOUNT_NOT_EXIST(
            "11", "account.not.exist"), MOBILE_ALREADY_EXIST("12", "mobile.already.exist"), REGISTER_PARAM_INVALID(
            "13", "register.param.invalid"), EMAIL_ALREADY_EXIST("14", "email.already.exist"), PASSWORD_AUTHENTICATE_INVALID(
            "15", "password.authenticate.invalid"), OPENID_FORMAT_INVALID("16", "openid.format.invalid"), PROVIDER_FORMAT_INVALID(
            "17", "provider.format.invalid"), SNSACCOUNT_ALREADY_EXIST("18", "snsaccount.already.exist"), EMAIL_NOT_VERIFIED(
            "19", "email.not.verified"), CASTOKEN_AUTHENTICATE_FAILED("20", "cas.authenticate.fail"), REQUEST_NOT_AUTHORIZED(
            "21", "request.not.authorized"), QUERY_PARAM_INVALID("22", "query.param.invalid"), INPUT_PARAM_ERROR(
            "1001", "parameter.error"), TRANSCODER_NAME_CONFLICT("1002", "transcoder.name.conflict"), RECORD_NOT_EXIST(
            "1003", "record.not.exist"), TRANSCODING_FAILURE("1005", "vas_transcoding_failed"), TRANSCODING_WORKING(
            "1006", "vas.transcoding.workingnow"), CHANNEL_NAME_CONFLICT("1007", "channel.name.conflict"), SYSTEM_ERROR(
            "1100", "system.error"), EVENT_END_ERROR("1101", "event.end.already"), CHANNEL_IS_OCCUPIED("1102",
            "channel.occupied"), TRANSCODER_IS_OCCUPIED("1103", "transcoder.occupied"), CHANNEL_IS_WORKING("1104",
            "channel.working.now"), IP_CONFLICT("1105", "ip.conflict"), OPERATION_TIMEOUT("1106", "operation.timeout"), LIVE_EVENT_TIMEERROR(
            "1106", "event.time.error"), IDLE_CHANNEL_INSUFFICIENT__ERROR("1201", "resource.idle.channel.insufficient"), ORIGINAL_CHANNEL_BUSY__ERROR(
            "1205", "resource.original.channel.busy"), TIME_FORMAT_ERROR("1301", "time.format.error");

    private String code;

    private String key;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private ReturnCode(String code, String key) {
        this.code = code;
        this.key = key;
    }
}