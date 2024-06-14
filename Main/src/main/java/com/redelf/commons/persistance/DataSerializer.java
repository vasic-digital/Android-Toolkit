package com.redelf.commons.persistance;

import android.text.TextUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.redelf.commons.logging.Console;
import com.redelf.commons.persistance.base.Serializer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DataSerializer implements Serializer {

    /*
        TODO: Create a flavor that uses Jackson lib for stream-like serialization / deserialization
    */
    private final Gson gsn;

    {

        gsn = new Gson();
    }

    @Override
    public <T> String serialize(byte[] cipherText, T originalGivenValue) {

        if (cipherText == null || cipherText.length == 0) {

            return null;
        }

        if (originalGivenValue == null) {

            return null;
        }

        Class<?> keyClassName = null;
        Class<?> valueClassName = null;

        char dataType;

        if (List.class.isAssignableFrom(originalGivenValue.getClass())) {

            List<?> list = (List<?>) originalGivenValue;
            if (!list.isEmpty()) {
                keyClassName = list.get(0).getClass();
            }
            dataType = DataInfo.TYPE_LIST;

        } else if (Map.class.isAssignableFrom(originalGivenValue.getClass())) {

            dataType = DataInfo.TYPE_MAP;
            Map<?, ?> map = (Map<?, ?>) originalGivenValue;

            if (!map.isEmpty()) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    keyClassName = entry.getKey().getClass();
                    valueClassName = entry.getValue().getClass();
                    break;
                }
            }

        } else if (Set.class.isAssignableFrom(originalGivenValue.getClass())) {

            Set<?> set = (Set<?>) originalGivenValue;
            if (!set.isEmpty()) {
                Iterator<?> iterator = set.iterator();
                if (iterator.hasNext()) {
                    keyClassName = iterator.next().getClass();
                }
            }
            dataType = DataInfo.TYPE_SET;

        } else {

            dataType = DataInfo.TYPE_OBJECT;
            keyClassName = originalGivenValue.getClass();
        }

        final DataInfo dataInfo = new DataInfo(

                cipherText,
                dataType,
                keyClassName != null ? keyClassName.getName() : null,
                valueClassName != null ? valueClassName.getName() : null,
                keyClassName,
                valueClassName
        );

        try {

            return gsn.toJson(dataInfo);

        } catch (OutOfMemoryError e) {

            Console.error(e);

            FirebaseCrashlytics.getInstance().recordException(e);

        } catch (Exception e) {

            Console.error(e);
        }

        return null;
    }

    @Override
    public DataInfo deserialize(String serializedText) {

        if (TextUtils.isEmpty(serializedText)) {

            return null;
        }

        try {

            final DataInfo dataInfo = gsn.fromJson(serializedText, DataInfo.class);

            if (dataInfo.getKeyClazzName() != null) {
                try {
                    dataInfo.setKeyClazz(Class.forName(dataInfo.getKeyClazzName()));
                } catch (ClassNotFoundException e) {
                    Console.error(e);
                }
            }

            if (dataInfo.getValueClazzName() != null) {
                try {
                    dataInfo.setValueClazz(Class.forName(dataInfo.getValueClazzName()));
                } catch (ClassNotFoundException e) {
                    Console.error(e);
                }
            }

            return dataInfo;

        } catch (JsonSyntaxException e) {

            Console.error(e);
        }

        return null;
    }
}
