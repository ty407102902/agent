package com.fgnb.model;

import lombok.Data;
import java.util.Date;

/**
 * Created by jiangyitao.
 */
@Data
public class Device {

    public static final Integer ANDROID = 1;
    public static final Integer IOS = 2;

    public static final Integer OFFLINE_STATUS = 0;
    public static final Integer USING_STATUS = 1;
    public static final Integer IDLE_STATUS = 2;

    public static final Integer STF_SUCCESS_STATUS = 1;
    public static final Integer MACACA_SUCCESS_STATUS = 1;

    private String id;
    private String name;
    private String agentIp;
    private Integer agentPort;
    private String systemVersion;
    private String cpuInfo;
    private String memSize;
    private Integer screenWidth;
    private Integer screenHeight;
    private String imgUrl;
    private Integer platform;
    private Integer status;
    private Integer stfStatus;
    private Integer macacaStatus;
    private Date lastOnlineTime;
    private Date lastOfflineTime;
    private String username;
    private Date createTime;
}