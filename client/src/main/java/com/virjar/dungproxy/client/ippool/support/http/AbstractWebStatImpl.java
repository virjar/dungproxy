/*
 * Copyright 1999-2101 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.virjar.dungproxy.client.ippool.support.http;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.virjar.dungproxy.client.ippool.support.http.stat.WebAppStat;
import com.virjar.dungproxy.client.ippool.support.http.stat.WebSessionStat;
import com.virjar.dungproxy.client.util.DruidWebUtils;

public class AbstractWebStatImpl {

    public final static int                DEFAULT_MAX_STAT_SESSION_COUNT = 1000 * 1;

    protected WebAppStat webAppStat                     = null;

    protected boolean                      sessionStatEnable              = true;
    protected int                          sessionStatMaxCount            = DEFAULT_MAX_STAT_SESSION_COUNT;
    protected boolean                      createSession                  = false;
    protected boolean                      profileEnable                  = false;

    protected String contextPath;

    protected String principalSessionName;
    protected String principalCookieName;
    protected String realIpHeader;

    public boolean isSessionStatEnable() {
        return sessionStatEnable;
    }

    public void setSessionStatEnable(boolean sessionStatEnable) {
        this.sessionStatEnable = sessionStatEnable;
    }

    public boolean isProfileEnable() {
        return profileEnable;
    }

    public void setProfileEnable(boolean profileEnable) {
        this.profileEnable = profileEnable;
    }

    public String getContextPath() {
        return contextPath;
    }

    public int getSessionStatMaxCount() {
        return sessionStatMaxCount;
    }

    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    public String getPrincipalSessionName() {
        return principalSessionName;
    }

    public String getPrincipalCookieName() {
        return principalCookieName;
    }

    public WebSessionStat getSessionStat(HttpServletRequest request) {
        if (!isSessionStatEnable()) {
            return null;
        }

        WebSessionStat sessionStat = null;
        String sessionId = getSessionId(request);
        if (sessionId != null) {
            sessionStat = webAppStat.getSessionStat(sessionId, true);
        }

        if (sessionStat != null) {
            long currentMillis = System.currentTimeMillis();

            String userAgent = request.getHeader("user-agent");

            if (sessionStat.getCreateTimeMillis() == -1L) {
                HttpSession session = request.getSession(false);

                if (session != null) {
                    sessionStat.setCreateTimeMillis(session.getCreationTime());
                } else {
                    sessionStat.setCreateTimeMillis(currentMillis);
                }

                webAppStat.computeUserAgent(userAgent);
                webAppStat.incrementSessionCount();
            }

            sessionStat.setUserAgent(userAgent);

            String ip = getRemoteAddress(request);

            sessionStat.addRemoteAddress(ip);
        }

        return sessionStat;
    }

    protected String getRemoteAddress(HttpServletRequest request) {
        String ip = null;
        if (this.realIpHeader != null && this.realIpHeader.length() != 0) {
            ip = request.getHeader(realIpHeader);
        }
        if (ip == null || ip.length() == 0) {
            ip = DruidWebUtils.getRemoteAddr(request);
        }
        return ip;
    }

    public String getSessionId(HttpServletRequest httpRequest) {
        String sessionId = null;

        HttpSession session = httpRequest.getSession(createSession);
        if (session != null) {
            sessionId = session.getId();
        }

        return sessionId;
    }

    public String getPrincipal(HttpServletRequest httpRequest) {
        if (principalSessionName != null) {
            HttpSession session = httpRequest.getSession(createSession);
            if (session == null) {
                return null;
            }

            Object sessionValue = session.getAttribute(principalSessionName);

            if (sessionValue == null) {
                return null;
            }

            return sessionValue.toString();
        }

        if (principalCookieName != null && httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if (principalCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
