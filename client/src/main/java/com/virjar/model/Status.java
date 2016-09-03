package com.virjar.model;

/**
 * 用于表示此类是状态类, 必须包含状态码 {@code code()} 以及状态描述 {@code desc()}
 */
interface Status {

    int code();

    String desc();
}
