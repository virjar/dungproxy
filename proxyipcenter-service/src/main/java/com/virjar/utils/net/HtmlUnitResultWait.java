package com.virjar.utils.net;

public interface HtmlUnitResultWait {

    /*
     * time out in millisecond
     */
    void setTimeOut(long timeOut);

    /*
     * time out in millisecond
     */
    long getTimeOut();

    /*
     * test if JavaScript execute finish,(JavaScript would not execute finish definition because of endless loop,so we
     * push a HTML stream to you,you can check DOM tree to decide if task is complement)
     */
    boolean canReturn(String html, String url);

    /*
     * progress will continue both time out and JavaScipt execute finished.so there is a execute code to describe
     * execute state
     */
    int getExcuteCode();
}
