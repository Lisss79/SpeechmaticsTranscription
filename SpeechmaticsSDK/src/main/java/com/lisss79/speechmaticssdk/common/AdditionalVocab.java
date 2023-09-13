package com.lisss79.speechmaticssdk.common;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.CONTENT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.SOUNDS_LIKE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeSet;

/**
 * Дополнительный словарь для распознавания - только чтение
 */
public class AdditionalVocab {

    private String content;
    private TreeSet<String> soundsLike;

    public AdditionalVocab(String content, TreeSet<String> soundsLike) {
        this.content = content;
        this.soundsLike = soundsLike;
    }

    public AdditionalVocab(JSONObject json) {
        this("", new TreeSet<>());
        try {
            parseJSON(json);
        } catch (JSONException | DateTimeParseException e) {
            e.printStackTrace();
        }
    }

    public AdditionalVocab(String response) {
        this("", new TreeSet<>());
        JSONObject json;
        try {
            json = new JSONObject(response);
            parseJSON(json);
        } catch (JSONException | DateTimeParseException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException {
        if (json.has(CONTENT)) content = json.getString(CONTENT);
        if (json.has(SOUNDS_LIKE)) {
            JSONArray slA = (JSONArray) json.get(SOUNDS_LIKE);
            TreeSet<String> slT = new TreeSet<>();
            for(int index = 0; index < slA.length(); index++) {
                slT.add(slA.getString(index));
            }
            soundsLike = slT;
        }
    }


    public static AdditionalVocab[] fromMap(Map<String, TreeSet<String>> map) {
        AdditionalVocab[] addVocs = new AdditionalVocab[map.size()];
        int index = 0;
        for (String key : map.keySet()) {
            TreeSet<String> values = new TreeSet<>();
            values.addAll(map.get(key));
            addVocs[index] = new AdditionalVocab(key, values);
            index++;
        }
        return addVocs;
    }

    @Nullable
    public static AdditionalVocab[] fromString(String response) {
        JSONObject json;
        try {
            JSONArray array = new JSONArray(response);
            AdditionalVocab[] addVocs = new AdditionalVocab[array.length()];
            for(int index = 0; index < array.length(); index++) {
                json = (JSONObject) array.get(index);
                addVocs[index] = new AdditionalVocab(json);
            }
            return  addVocs;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        if(soundsLike != null && soundsLike.size() > 0 && !soundsLike.first().isEmpty()) {
            return CONTENT + ": " + content + ", " +
                    SOUNDS_LIKE + ": " + soundsLike;
        } else {
            return CONTENT + ": " + content;
        }
    }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray(soundsLike);
            json.put(CONTENT, content);
            if(jsonArray.length() > 0
                    && !jsonArray.getString(0).isEmpty()) json.put(SOUNDS_LIKE, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray(soundsLike);
            json.put(CONTENT, content);
            if(jsonArray.length() > 0
                    && !jsonArray.getString(0).isEmpty()) json.put(SOUNDS_LIKE, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String toJsonString(@NonNull AdditionalVocab[] addVocs) {
        JSONArray array = new JSONArray();
        for (AdditionalVocab addVoc : addVocs) {
            array.put(addVoc.toJson());
        }
        return array.toString();
    }

    public static JSONArray toJsonArray(@NonNull AdditionalVocab[] addVocs) {
        JSONArray array = new JSONArray();
        for (AdditionalVocab addVoc : addVocs) {
            array.put(addVoc.toJson());
        }
        return array;
    }

}
