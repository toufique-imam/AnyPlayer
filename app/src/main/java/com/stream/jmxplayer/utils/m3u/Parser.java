package com.stream.jmxplayer.utils.m3u;

import com.stream.jmxplayer.model.PlayerModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    static String getName(String req) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!req.contains(",") && !req.contains(".")) {
            for (int i = req.length() - 1; i > -1; i--) {
                if (!(req.charAt(i) == ' ') && !Character.isDigit(req.charAt(i)) && !Character.isAlphabetic(req.charAt(i)))
                    break;
                stringBuilder.append(req.charAt(i));

            }
        } else {
            for (int i = req.length() - 1; i > -1; i--) {
                if (req.charAt(i) == '.' || req.charAt(i) == ',')
                    break;
                stringBuilder.append(req.charAt(i));

            }
        }
        return stringBuilder.reverse().toString();
    }

    static String getLanguage(String req) {
        Pattern p = Pattern.compile("(?<=tvg-language=\")[^\"]+");
        Matcher m = p.matcher(req);
        if (m.find()) return m.group(0);
        else return "";
    }

    static String getLogo(String req) {
        Pattern p = Pattern.compile("(?<=tvg-logo=\")[^\"]+");
        Matcher m = p.matcher(req);
        if (m.find()) return m.group(0);
        else return "";
    }

    static String getVlcOption(String req, String name) {
        final String regex = "#EXTVLCOPT:" + name + "=(.*)";
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(req);
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(1);
        }
        return "";
    }

    public static ArrayList<PlayerModel> ParseM3UString(String req, String user_agent_input) {
        ArrayList<PlayerModel> channelModels = new ArrayList<>();
        PlayerModel channelModel;
        String[] lines = req.split("\\r?\\n");
        Map<String, String> mp = new HashMap<>();

        for (int i = 0; i + 1 < lines.length; i++) {
//            System.out.println(lines[i]);
            if (!lines[i].startsWith("#EXTINF")) {
                continue;
            }
            String channelInfo = lines[i];
            String userAgent = "";
            String referrer = "";
            if (lines[i + 1].startsWith("#EXTVLCOPT")) {
                userAgent = getVlcOption(lines[i + 1], "http-user-agent");
                referrer = getVlcOption(lines[i + 1], "http-referrer");
                i++;
            }
            if (userAgent.isEmpty()) userAgent = user_agent_input;
            String channelLink = lines[i + 1];
            //    GlobalFunctions.Companion.logger("parser", channelInfo + "\n" + channelLink);
            String language = getLanguage(channelInfo);
            String logo = getLogo(channelInfo);
            String channelName = getName(channelInfo);

            if (language.isEmpty()) language = "Server-1";
            if (logo.isEmpty())
                logo = channelLink;
            mp.put("User-Agent", userAgent);
            if (!referrer.isEmpty())
                mp.put("Referrer", referrer);
            channelModel = new PlayerModel(
                    0, channelLink, logo, channelLink, userAgent
                    , "", "", 0, channelName, channelName + " " + language, language, logo, PlayerModel.STREAM_ONLINE_LIVE, mp);
            channelModel.setId(PlayerModel.Companion.getId(channelLink, channelName));
            channelModels.add(channelModel);
            i++;
        }
        return channelModels;
    }
}
