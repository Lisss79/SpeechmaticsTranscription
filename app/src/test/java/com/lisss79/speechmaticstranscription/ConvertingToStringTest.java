package com.lisss79.speechmaticstranscription;

import static org.junit.Assert.assertEquals;

import com.lisss79.speechmaticssdk.batch.data.ErrorMessage;
import com.lisss79.speechmaticssdk.batch.data.JobConfig;
import com.lisss79.speechmaticssdk.batch.data.JobDetails;
import com.lisss79.speechmaticssdk.common.OperatingPoint;
import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.real_time.data.StartRecognition;

import org.junit.Test;

public class ConvertingToStringTest {
    private final SpeechmaticsBatchSDK sm = new SpeechmaticsBatchSDK();

    @Test
    public void durationToStringTest() {
        String result1 = SpeechmaticsBatchSDK.durationToString("18122");
        assertEquals(result1, "5ч 02м 02с");
        String result2 = SpeechmaticsBatchSDK.durationToString("");
        assertEquals(result2, "");
        String result3 = SpeechmaticsBatchSDK.durationToString(null);
        assertEquals(result3, "");
        String result4 = SpeechmaticsBatchSDK.durationToString("-10");
        assertEquals(result4, "00с");
    }

    @Test
    public void durationHrsToStringTest() {
        String result1 = SpeechmaticsBatchSDK.durationHoursToString("25.1780971");
        assertEquals(result1, "1дн 01ч 10м 41с");
        String result2 = SpeechmaticsBatchSDK.durationHoursToString("");
        assertEquals(result2, "");
        String result3 = SpeechmaticsBatchSDK.durationHoursToString(null);
        assertEquals(result3, "");
        String result4 = SpeechmaticsBatchSDK.durationHoursToString("-10");
        assertEquals(result4, "00с");
    }

    @Test
    public void jobConfigTest() {
        JobConfig jc1 = new JobConfig();
        System.out.println("jc1= " + jc1);
        JobConfig jc2 = new JobConfig.Builder()
                .url("https://drive.google.com/file/d/118h7v8zEzj-z2RbI5BNRE2dxOJXKwVO1/view?usp=share_link").build();
        System.out.println("jc2= " + jc2);
        //assertEquals(jc1.toString(), jc2.toString());

    }

    @Test
    public void jobDetailsTest() {
        String response = "{\n" +
                "  \"job\": {\n" +
                "    \"config\": {\n" +
                "      \"fetch_data\": {\n" +
                "        \"url\": \"https://example.com/average-files/punctuation1.mp3\"\n" +
                "      },\n" +
                "      \"notification_config\": [\n" +
                "        {\n" +
                "          \"contents\": [\"jobinfo\"],\n" +
                "          \"url\": \"https://example.com/\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"transcription_config\": {\n" +
                "        \"language\": \"de\"\n" +
                "      },\n" +
                "      \"type\": \"transcription\"\n" +
                "    },\n" +
                "    \"created_at\": \"2021-07-19T12:55:03.754Z\",\n" +
                "    \"data_name\": \"\",\n" +
                "    \"duration\": \"0\",\n" +
                "    \"errors\": [\n" +
                "      {\n" +
                "        \"message\": \"unable to fetch audio: http status code 404\",\n" +
                "        \"timestamp\": \"2021-07-19T12:55:05.425Z\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"message\": \"unable to fetch audio: http status code 404\",\n" +
                "        \"timestamp\": \"2021-07-19T12:55:07.649Z\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"message\": \"unable to fetch audio: http status code 404\",\n" +
                "        \"timestamp\": \"2021-07-19T12:55:17.665Z\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"message\": \"unable to fetch audio: http status code 404\",\n" +
                "        \"timestamp\": \"2021-07-19T12:55:37.643Z\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"id\": \"a81ko4eqjl\",\n" +
                "    \"status\": \"rejected\"\n" +
                "  }\n" +
                "}\n";
        JobDetails jd = new JobDetails(response);
        System.out.println("jd= " + jd);
    }

    @Test
    public void errorMessageTest() {
        String response = "{\"code\":\"200\",\"detail\":\"account is not allowed to create a job at the moment: " +
                "This request would exceed your limit for Enhanced Model transcription in the current month. " +
                "Your limit is 2 hours.\",\"error\":\"Forbidden\"}";
        ErrorMessage em = new ErrorMessage(response);
        System.out.println("em= " + em);
    }

    @Test
    public void startRecognitionTest() {
        StartRecognition.Builder builder = new StartRecognition.Builder();
        StartRecognition sr = builder.diarization(StartRecognition.TranscriptionConfigRT.DiarizationRT.SPEAKER)
                .operatingPoint(OperatingPoint.STANDARD)
                .sampleRate(16000)
                .encoding(StartRecognition.AudioFormat.Encoding.MULAW)
                .entities(true)
                .build();
        System.out.println("sr= " + sr.toJsonString());
    }

}