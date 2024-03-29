package ru.artfect.wynnlang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;

import ru.artfect.wynnlang.WynnLang.MessageType;

public class Log {
    private static final String LOG_PATH = WynnLang.mc.mcDataDir + "/config/WynnLang/Logs/";
    private static HashMap<MessageType, List<String>> oldStr = new HashMap<MessageType, List<String>>();
    private static HashMap<MessageType, List<String>> newStr = new HashMap<MessageType, List<String>>();

    public static void init() throws IOException {
        Path lpath = Paths.get(LOG_PATH);
        if (!Files.exists(lpath)) {
            Files.createDirectories(lpath);
        }
        EnumSet.allOf(MessageType.class).forEach(type -> loadLogFile(type));

        Multithreading.schedule(() -> {
            try {
                saveAndSend();
            } catch (IOException e) {

            }
        }, 2, 2, TimeUnit.MINUTES);
    }

    public static void saveAndSend() throws ClientProtocolException, IOException {
        if (!WynnLang.onWynncraft) {
            return;
        }
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("UUID", WynnLang.uuid));
        params.add(new BasicNameValuePair("ver", WynnLang.VERSION));

        for (MessageType s : newStr.keySet()) {
            List<String> strList = newStr.get(s);
            if (strList.isEmpty()) {
                continue;
            }
            params.add(new BasicNameValuePair(s.name(), new Gson().toJson(strList)));
            try {
                Files.write(Paths.get(LOG_PATH + s.name() + ".txt"), strList, StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (IOException e) {

            }
            strList.clear();
        }

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://" + WynnLang.SERVER);
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        CloseableHttpResponse response = client.execute(httpPost);
        client.close();
    }

    private static void loadLogFile(MessageType type) {
        try {
            Path p = Paths.get(LOG_PATH + type + ".txt");
            if (p.toFile().exists()) {
                oldStr.put(type, Files.readAllLines(p));
            } else {
                oldStr.put(type, new ArrayList<String>());
                p.toFile().createNewFile();
            }
            newStr.put(type, new ArrayList<String>());
        } catch (IOException e) {

        }
    }

    public static void addString(MessageType type, String str) {
        if (WynnLang.logging && !oldStr.get(type).contains(str)) {
            oldStr.get(type).add(str);
            newStr.get(type).add(str);
        }
    }
}
