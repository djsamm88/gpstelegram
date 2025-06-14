package com.example.gpsstandalone;

import android.os.Build;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class TLSHelper {
    public static SSLSocketFactory enableTls12() {
        try {
            if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                return sc.getSocketFactory();
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
