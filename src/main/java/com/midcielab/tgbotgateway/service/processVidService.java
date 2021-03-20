package com.midcielab.tgbotgateway.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.midcielab.tgbotgateway.model.Model;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class processVidService {

    private String feedbackUrl = "https://script.google.com/macros/s/xxxxxx/exec";
    private String tgUrl = "https://api.telegram.org/botxxxxxxxxxx:token/sendVideo";
    private OkHttpClient client = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.HTTP_1_1)).build();
    private String pendingReason = "";

    public processVidService(List<Model> modelInput) {
        for (Model model : modelInput) {            
            if (downloadWithFFMPEG(model)) {
                System.out.println("Start to procee sendTg");
                if (tgSendVid(model)) {
                    System.out.println("Start to procee sendTgFb");
                    tgSendFeedback(model, "ok");
                } else {
                    System.out.println("Start to procee sendTgFb");
                    tgSendFeedback(model, pendingReason);
                }
            }
        }
    }

    private boolean downloadWithFFMPEG(Model model) {
        try {
            Files.writeString(Paths.get("temp.m3u8"), model.getM3u8());
            String[] cmd = { "ffmpeg", "-loglevel", "quiet","-protocol_whitelist", "file,http,https,tcp,tls", "-y", "-i", "temp.m3u8", "-c",
                    "copy", "output.mp4" };
            Process p = Runtime.getRuntime().exec(cmd);
            
            InputStream stderr = p.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<ERROR>");
            while ( (line = br.readLine()) != null)
                System.out.println(line);
            System.out.println("</ERROR>");
            int exitVal = p.waitFor();
            System.out.println("Process exitValue: " + exitVal);            
        } catch (IOException e) {
            e.printStackTrace();
            pendingReason = "ffmpeg process exception";
            return false;
        } catch (InterruptedException e) {
             e.printStackTrace();
             pendingReason = "ffmpeg process exception";
             return false;
        }
        return true;
    }

    private boolean tgSendVid(Model model) {
        File file = new File("output.mp4");
        if ((file.length() / 1048576) < 50) {
            RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("video", "output.mp4", RequestBody.create(MediaType.parse("video/mp4"), file))
                    .addFormDataPart("chat_id", model.getChatId()).build();
            Request request = new Request.Builder().url(tgUrl).post(formBody).build();
            Response response;
            try {
                response = client.newCall(request).execute();
                if (response.code() == 200) {
                    response.close();
                    return true;
                } else {
                    pendingReason = "send vid to tg exception";    
                }
            } catch (IOException e) {
                pendingReason = "send vid to tg exception";
                e.printStackTrace();
            }
        } else {
            pendingReason = "file size bigger than 50 mb";
        }
        return false;
    }

    private void tgSendFeedback(Model model, String result) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode feedback = mapper.createObjectNode();
        feedback.put("userId", model.getUserId());
        feedback.put("chatId", model.getChatId());
        feedback.put("link", model.getLink());
        feedback.put("result", result);
        String json;
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(feedback);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
            Request reqFeedback = new Request.Builder().url(feedbackUrl).post(body).build();
            Response respFeedback = client.newCall(reqFeedback).execute();                
            System.out.println(respFeedback.code());
            System.out.println(respFeedback.body().string());
            respFeedback.close();
        } catch (JsonProcessingException e) {            
            e.printStackTrace();
        } catch (IOException e) {            
            e.printStackTrace();
        }
    }
}
