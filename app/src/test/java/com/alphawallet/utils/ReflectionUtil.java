package com.alphawallet.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtil
{
    public static void setFinalFieldTo(Class clazz, String fieldName, boolean value) throws NoSuchFieldException, IllegalAccessException
    {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, value);
    }
}
