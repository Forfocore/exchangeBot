package com.example.exchange;

import com.example.exchange.config.BotConfig;
import com.example.exchange.model.CurrencyModel;
import com.example.exchange.service.CurrencyService;
import lombok.AllArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Scanner;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            CurrencyModel currencyModel = new CurrencyModel();
            String currency = "";

            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                URL url = new URL("https://www.cbr-xml-daily.ru/daily_json.js");
                Scanner scanner = new Scanner((InputStream) url.getContent());
                String result = "";
                while (scanner.hasNext()) {
                    result += scanner.nextLine();
                }
                JSONObject object = new JSONObject(result);
                JSONArray arr = object.getJSONObject("Valute").toJSONArray(object.getJSONObject("Valute").names());
                String list;

                switch (messageText) {
                    case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/all":


                        list = "";
                        for (int i = 0; i <arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String code = obj.getString("CharCode");
                            String name = obj.getString("Name");
                            name = name.replace("И", "и");
                            list = list + code + " - " + name + "\n";
                        }
                        byte[] array3 = list.getBytes();
                        sendMessage(chatId, "Список всех торгуемых валют:" + "\n" + new String(array3, StandardCharsets.UTF_8));
                        break;

                    case "/buy":

                         list = "";
                         for (int i = 0; i < arr.length(); i++) {
                             JSONObject obj = arr.getJSONObject(i);
                             Double previous = obj.getDouble("Previous");
                             Double now = obj.getDouble("Value");
                             if (now/previous < 1) {
                                 String code = obj.getString("CharCode");
                                 String name = obj.getString("Name");
                                 name = name.replace("И", "и");
                                 list = list + code + " - " + name + "\n";
                             }
                         }
                         byte[] array4 = list.getBytes();
                         sendMessage(chatId, "Список выгодных к покупке валют по сравнению с предыдущим значением:" + "\n" + new String(array4, StandardCharsets.UTF_8));
                         break;

                    case "/sell":

                        list = "";
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            Double previous = obj.getDouble("Previous");
                            Double now = obj.getDouble("Value");
                            if (now/previous > 1) {
                                String code = obj.getString("CharCode");
                                String name = obj.getString("Name");
                                name = name.replace("И", "и");
                                list = list + code + " - " + name + "\n";
                            }
                        }
                        byte[] array5 = list.getBytes();
                        sendMessage(chatId, "Список выгодных к продаже валют по сравнению с предыдущим значением:" + "\n" + new String(array5, StandardCharsets.UTF_8));
                        break;

                    default:
                        try {
                            currency = CurrencyService.getCurrencyRate(messageText, currencyModel);

                        } catch (JSONException e) {
                            String data = "Мы не смогли найти вашу валюту на ЦБ РФ." + "\n" +
                                    "Чтобы получить текущий курс валюты к рублю " + "\n" +
                                    "введите символьный код валюты, являющейся официальной на бирже." + "\n" +
                                    "Например: USD, EUR." + "\n" +
                                    "Чтобы узнать символьный код всех доступных валют введи команду /all.";
                            sendMessage(chatId, data);
                        } catch (ParseException e) {
                            throw new RuntimeException("Unable to parse date");
                        } catch (IOException e) {
                            String data = "Мы не смогли найти вашу валюту на ЦБ РФ." + "\n" +
                                    "Чтобы получить текущий курс валюты к рублю " + "\n" +
                                    "введите символьный код валюты, являющейся официальной на бирже." + "\n" +
                                    "Например: USD, EUR." + "\n" +
                                    "Чтобы узнать символьный код всех доступных валют введи команду /all.";
                            sendMessage(chatId, data);

                        }
                        sendMessage(chatId, currency);
                }
            }

        }
        catch (IOException e) {

        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer1 = "Привет, ";
        String answer2 = ", рад видеть тебя здесь!" + "\n" +
                "Чтобы получить текущий курс валюты к рублю " + "\n" +
                "введите символьный код валюты, являющейся официальной на бирже." + "\n" +
                "Например: USD, EUR." + "\n" +
                "Чтобы узнать символьный код всех доступных валют введи команду /all." + "\n" +
                "Чтобы узнать список валют, выгодных к продаже, введи команду /sell." + "\n" +
                "Чтобы узнать список валют, выгодных к покупке, введи команду /buy.";
        sendMessage(chatId, answer1 + name + answer2);
    }

    private void sendMessage(Long chatId, String textToSend){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }
}
