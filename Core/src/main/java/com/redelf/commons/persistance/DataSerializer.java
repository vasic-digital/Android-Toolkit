package com.redelf.commons.persistance;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

class DataSerializer implements Serializer {

    private final Gson gsn;

    {

        gsn = new Gson();
    }

    @Override
    public <T> String serialize(byte[] cipherText, T originalGivenValue) {

        final String tag = "Serialize ::";

        Timber.v("%s START", tag);

        PersistenceUtils.checkNullOrEmpty("Cipher text", cipherText);
        PersistenceUtils.checkNull("Value", originalGivenValue);

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
            Map<?, ?> map = (Map) originalGivenValue;

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

        return gsn.toJson(dataInfo);
    }

    @Override
    public DataInfo deserialize(String serializedText) {

        final String tag = "Deserialize ::";

        Timber.v("%s START", tag);

        try {

            final DataInfo dataInfo = gsn.fromJson(serializedText, DataInfo.class);

            if (dataInfo.getKeyClazzName() != null) {
                try {
                    dataInfo.setKeyClazz(Class.forName(dataInfo.getKeyClazzName()));
                } catch (ClassNotFoundException e) {
                    Timber.e(e);
                }
            }

            if (dataInfo.getValueClazzName() != null) {
                try {
                    dataInfo.setValueClazz(Class.forName(dataInfo.getValueClazzName()));
                } catch (ClassNotFoundException e) {
                    Timber.e(e);
                }
            }

            Timber.v("%s END: %s", tag, dataInfo);

            return dataInfo;

        } catch (JsonSyntaxException e) {

            Timber.e(e);
        }

        return null;
    }
}
