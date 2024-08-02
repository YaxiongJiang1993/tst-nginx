package com.tst.nginx.test;

import lombok.Data;

@Data
public class NginxConfig {

    private Integer httpServerListen;

    private String httpServerRoot;
}
