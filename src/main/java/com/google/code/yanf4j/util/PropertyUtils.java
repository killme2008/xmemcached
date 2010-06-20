package com.google.code.yanf4j.util;

import java.util.Properties;


public class PropertyUtils {

    public static int getPropertyAsInteger(Properties props, String propName) {
        return Integer.parseInt(PropertyUtils.getProperty(props, propName));
    }


    public static String getProperty(Properties props, String name) {
        return props.getProperty(name).trim();
    }


    public static boolean getPropertyAsBoolean(Properties props, String name) {
        return Boolean.valueOf(getProperty(props, name));
    }


    public static long getPropertyAsLong(Properties props, String name) {
        return Long.parseLong(getProperty(props, name));
    }


    public static short getPropertyAsShort(Properties props, String name) {
        return Short.parseShort(getProperty(props, name));
    }


    public static byte getPropertyAsByte(Properties props, String name) {
        return Byte.parseByte(getProperty(props, name));
    }
}
