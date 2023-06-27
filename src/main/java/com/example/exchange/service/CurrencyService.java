package com.example.exchange.service;

import com.example.exchange.model.CurrencyModel;
import com.fasterxml.jackson.annotation.JsonAlias;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class CurrencyService {

    public static String getCurrencyRate(String message, CurrencyModel model) throws IOException, ParseException, JSONException {
        URL url = new URL("https://www.cbr-xml-daily.ru/daily_json.js");
        Scanner scanner = new Scanner((InputStream) url.getContent());
        String result = "";
        while (scanner.hasNext()){
            result +=scanner.nextLine();
        }
        JSONObject object = new JSONObject(result);
        model.setDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(object.getString("Timestamp")));
        model.setCur_Name(object.getJSONObject("Valute").getJSONObject(message).getString("CharCode"));
        model.setCur_OfficialRate(object.getJSONObject("Valute").getJSONObject(message).getDouble("Value"));
        model.setCur_Scale(object.getJSONObject("Valute").getJSONObject(message).getInt("Nominal"));


        String data1 = "Официальный курс рубля к " + model.getCur_Name() + "\n" +
                "по дате: ";
        String data2 = "\n" + model.getCur_OfficialRate() + " RUB за " + model.getCur_Scale() + " " + model.getCur_Name();

        return data1 + getFormatDate(model) + data2;

    }

    private static String getFormatDate(CurrencyModel model) {
        return new SimpleDateFormat("dd MMM yyyy").format(model.getDate());
    }
}
