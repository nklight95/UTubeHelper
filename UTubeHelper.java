package com.nklight.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UTubeHelper {

    private static String URL_PART = "https://www.youtube.com/watch?v=";
    final static String UTF8 = "UTF-8";
    private static int CONNECTION_TIME_OUT_MILLI = 5000;
    private static int READ_TIME_OUT_MILLI = 5000;

    public static String getSingleDirectLink(String youtubeVideoId) {
        String data = getHtmlString(youtubeVideoId);
        List<String> urlList = sGet(data);
        String directLink = "";

        for (String url : urlList) {
            if (!TextUtils.isEmpty(url)) {
                directLink = url;
                break;
            }
        }

        return directLink;
    }

    public static List<String> getAvailableLinks(String youtubeVideoId) {
        String content = getHtmlString(youtubeVideoId);
        return sGet(content);
    }


    public static String getHtmlString(String uTubeVideoId) {

        String url = URL_PART + uTubeVideoId;
        HttpURLConnection conn = null;
        StringBuilder contents = new StringBuilder();
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECTION_TIME_OUT_MILLI);
            conn.setReadTimeout(READ_TIME_OUT_MILLI);

            InputStream is = conn.getInputStream();

            String enc = conn.getContentEncoding();

            if (enc == null) {
                Pattern p = Pattern.compile("charset=(.*)");
                Matcher m = p.matcher(conn.getHeaderField("Content-Type"));
                if (m.find()) {
                    enc = m.group(1);
                }
            }

            if (enc == null)
                enc = UTF8;

            BufferedReader br = new BufferedReader(new InputStreamReader(is, enc));

            String line;


            while ((line = br.readLine()) != null) {
                contents.append(line);
                contents.append("\n");

            }
        } catch (IOException e) {

        }

        return contents.toString();
    }

    public static List<String> sGet(String htmlString) {
        List<String> urlList = new ArrayList<>();
        Pattern urlEncode = Pattern.compile("\"url_encoded_fmt_stream_map\":\"([^\"]*)\"");
        Matcher urlEncodeMatch = urlEncode.matcher(htmlString);
        if (urlEncodeMatch.find()) {
            String url_encoded_fmt_stream_map;
            url_encoded_fmt_stream_map = urlEncodeMatch.group(1);
            Pattern encode = Pattern.compile("url=(.*)");
            Matcher encodeMatch = encode.matcher(url_encoded_fmt_stream_map);
            if (encodeMatch.find()) {
                String sLine = encodeMatch.group(1);
                String[] urlStrings = sLine.split("url=");
                for (String urlString : urlStrings) {
                    String url = null;
                    urlString = unescapeJavaString(urlString);
                    Pattern link = Pattern.compile("([^&,]*)[&,]");
                    Matcher linkMatch = link.matcher(urlString);
                    if (linkMatch.find()) {
                        url = linkMatch.group(1);
                        try {
                            url = URLDecoder.decode(url, UTF8);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    urlList.add(url);
                }
            }
        }

        Log.d("youtube", urlList.toString());

        return urlList;
    }

    public static String unescapeJavaString(String st) {

        StringBuilder sb = new StringBuilder(st.length());

        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == st.length() - 1) ? '\\' : st
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                            && st.charAt(i + 1) <= '7') {
                        code += st.charAt(i + 1);
                        i++;
                        if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                                && st.charAt(i + 1) <= '7') {
                            code += st.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= st.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + st.charAt(i + 2) + st.charAt(i + 3)
                                        + st.charAt(i + 4) + st.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String extractId(URL url) {
        {
            Pattern u = Pattern.compile("youtube.com/watch?.*v=([^&]*)");
            Matcher um = u.matcher(url.toString());
            if (um.find())
                return um.group(1);
        }

        {
            Pattern u = Pattern.compile("youtube.com/v/([^&]*)");
            Matcher um = u.matcher(url.toString());
            if (um.find())
                return um.group(1);
        }

        return null;
    }


    static class DecryptSignature {
        String sig;

        public DecryptSignature(String signature) {
            this.sig = signature;
        }

        String s(int b, int e) {
            return sig.substring(b, e);
        }

        String s(int b) {
            return sig.substring(b, b + 1);
        }

        String se(int b) {
            return s(b, sig.length());
        }

        String s(int b, int e, int step) {
            String str = "";

            while (b != e) {
                str += sig.charAt(b);
                b += step;
            }
            return str;
        }

        // https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/youtube.py
        String decrypt() {
            switch (sig.length()) {
                case 93:
                    return s(86, 29, -1) + s(88) + s(28, 5, -1);
                case 92:
                    return s(25) + s(3, 25) + s(0) + s(26, 42) + s(79) + s(43, 79) + s(91) + s(80, 83);
                case 91:
                    return s(84, 27, -1) + s(86) + s(26, 5, -1);
                case 90:
                    return s(25) + s(3, 25) + s(2) + s(26, 40) + s(77) + s(41, 77) + s(89) + s(78, 81);
                case 89:
                    return s(84, 78, -1) + s(87) + s(77, 60, -1) + s(0) + s(59, 3, -1);
                case 88:
                    return s(7, 28) + s(87) + s(29, 45) + s(55) + s(46, 55) + s(2) + s(56, 87) + s(28);
                case 87:
                    return s(6, 27) + s(4) + s(28, 39) + s(27) + s(40, 59) + s(2) + se(60);
                case 86:
                    return s(80, 72, -1) + s(16) + s(71, 39, -1) + s(72) + s(38, 16, -1) + s(82) + s(15, 0, -1);
                case 85:
                    return s(3, 11) + s(0) + s(12, 55) + s(84) + s(56, 84);
                case 84:
                    return s(78, 70, -1) + s(14) + s(69, 37, -1) + s(70) + s(36, 14, -1) + s(80) + s(0, 14, -1);
                case 83:
                    return s(80, 63, -1) + s(0) + s(62, 0, -1) + s(63);
                case 82:
                    return s(80, 37, -1) + s(7) + s(36, 7, -1) + s(0) + s(6, 0, -1) + s(37);
                case 81:
                    return s(56) + s(79, 56, -1) + s(41) + s(55, 41, -1) + s(80) + s(40, 34, -1) + s(0) + s(33, 29, -1)
                            + s(34) + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
                case 80:
                    return s(1, 19) + s(0) + s(20, 68) + s(19) + s(69, 80);
                case 79:
                    return s(54) + s(77, 54, -1) + s(39) + s(53, 39, -1) + s(78) + s(38, 34, -1) + s(0) + s(33, 29, -1)
                            + s(34) + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
            }

            throw new RuntimeException(
                    "Unable to decrypt signature, key length " + sig.length() + " not supported; retrying might work");
        }
    }
}
