package ir.atitec.signalgoApp;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;

import org.joda.time.LocalDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import ir.atitec.signalgo.models.ParameterInfo;

public class HttpClient {


    public final void Post(String url, ParameterInfo[] parameterInfoes) {

        String newLine = "\r\n";
        Uri uri = Uri.parse(url);

        Socket  tcpClient = null;
        try {
            tcpClient = new Socket(uri.getHost(), uri.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
/*            if (!tangible.StringHelper.isNullOrEmpty(KeyParameterName)) {
                ArrayList<Object> list = parameterInfoes.ToList();
                SignalGo.Shared.Models.ParameterInfo tempVar = new SignalGo.Shared.Models.ParameterInfo();
                tempVar.setName(KeyParameterName);
                tempVar.Value = SignalGo.Client.ClientSerializationHelper.SerializeObject(KeyParameterValue);
                list.add(tempVar);
                parameterInfoes = list.toArray(new Object[0]);
            }*/
            String boundary = "----------------------------" + Long.toHexString(LocalDateTime.now().toDateTime().getMillis());
            String headData = String.format("POST %1$s HTTP/1.1", uri.getPath()) + newLine + String.format("Host: %1$s", uri.getHost()) + newLine + String.format("Content-Type: multipart/form-data; boundary=%1$s", boundary) + newLine;
           /* if (RequestHeaders != null && RequestHeaders.Count > 0) {
                for (Map.Entry<String, String[]> item : RequestHeaders.entrySet()) {
                    if (!item.getKey().equals("host") && !item.getKey().equals("content-type") && !item.getKey().equals("content-length")) {
                        if (item.getValue() == null || item.getValue().length == 0) {
                            continue;
                        }
                        headData += item.getKey() + ": " + StringHelper.trimEnd(StringHelper.join(",", item.getValue())) + newLine;
                    }
                }
            }*/

            StringBuilder valueData = new StringBuilder();
            if (parameterInfoes != null) {
                String formdataTemplate = "Content-Disposition: form-data; name=\"%1$s\"\r\n\r\n%2$s";
                String boundaryinsert = "\r\n--" + boundary + "\r\n";
                for (ParameterInfo item : parameterInfoes) {
                    valueData.append(boundaryinsert + "\r\n");
                    valueData.append(String.format(formdataTemplate, item.getType(), item.getValue()));
                }
            }

            byte[] dataBytes = valueData.toString().getBytes("UTF-8");
            headData += String.format("Content-Length: %1$s", dataBytes.length) + newLine + newLine;

            byte[] headBytes = headData.toString().getBytes("UTF-8");

            try {
                OutputStream streamWriter = tcpClient.getOutputStream();
                InputStream streamReader = tcpClient.getInputStream();

                streamWriter.write(headBytes);
                streamWriter.write(dataBytes);
                byte[] resultBytes= new byte[1024*2];
                int readCount = streamReader.read(resultBytes,0,resultBytes.length);
                String str = new String(resultBytes, "UTF-8");
                Log.i("str", "Post: "+str);
                /*    ArrayList<String> lines = new ArrayList<String>();
                    String line = null;
                    do {
                        if (line != null) {
                            lines.add(line);
                        }
                        line = pipelineReader.ReadLine();
                    } while (!newLine.equals(line));
                    HttpClientResponse httpClientResponse = new HttpClientResponse();
                    httpClientResponse.Status = HttpStatusCode.valueOf(lines.get(0).split("[ ]", -1)[1]);
                    httpClientResponse.ResponseHeaders = SignalGo.Shared.Http.WebHeaderCollection.GetHttpHeaders(lines.Skip(1).ToArray());
                    int length = Integer.parseInt(httpClientResponse.ResponseHeaders["content-length"]);
                    byte[] result = new byte[length];
                    int readCount = 0;
                    while (readCount < length) {
                        byte[] bytes = new byte[512];
                        int readedCount = 0;
                        readedCount = pipelineReader.Read(bytes, bytes.length);
                        for (int i = 0; i < readedCount; i++) {
                            result[i + readCount] = bytes[i];
                        }
                        readCount += readedCount;
                    }
                    httpClientResponse.Data = Xml.Encoding.GetString(result);

                    return httpClientResponse;*/
                } catch (IOException e) {
e.printStackTrace();

            }

        } catch (UnsupportedEncodingException e) {
e.printStackTrace();

        } finally {

        }
//#endif
    }

    //----------------------------------------------------------------------------------------
//	Copyright © 2007 - 2018 Tangible Software Solutions, Inc.
//	This class can be used by anyone provided that the copyright notice remains intact.
//
//	This class is used to replicate some .NET string methods in Java.
//----------------------------------------------------------------------------------------
    public final static  class StringHelper {
        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'Substring' when 'start' is a method
        //	call or calculated value to ensure that 'start' is obtained just once.
        //------------------------------------------------------------------------------------
        public static String substring(String string, int start, int length) {
            if (length < 0)
                throw new IndexOutOfBoundsException("Parameter length cannot be negative.");

            return string.substring(start, start + length);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET static string method 'IsNullOrEmpty'.
        //------------------------------------------------------------------------------------
        public static boolean isNullOrEmpty(String string) {
            return string == null || string.length() == 0;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET static string method 'IsNullOrWhiteSpace'.
        //------------------------------------------------------------------------------------
        public static boolean isNullOrWhiteSpace(String string) {
            if (string == null)
                return true;

            for (int index = 0; index < string.length(); index++) {
                if (!Character.isWhitespace(string.charAt(index)))
                    return false;
            }

            return true;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET static string method 'Join' (2 parameter version).
        //------------------------------------------------------------------------------------
        public static String join(String separator, String[] stringArray) {
            if (stringArray == null)
                return null;
            else
                return join(separator, stringArray, 0, stringArray.length);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET static string method 'Join' (4 parameter version).
        //------------------------------------------------------------------------------------
        public static String join(String separator, String[] stringArray, int startIndex, int count) {
            String result = "";

            if (stringArray == null)
                return null;

            for (int index = startIndex; index < stringArray.length && index - startIndex < count; index++) {
                if (separator != null && index > startIndex)
                    result += separator;

                if (stringArray[index] != null)
                    result += stringArray[index];
            }

            return result;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'Remove' (1 parameter version).
        //------------------------------------------------------------------------------------
        public static String remove(String string, int start) {
            return string.substring(0, start);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'Remove' (2 parameter version).
        //------------------------------------------------------------------------------------
        public static String remove(String string, int start, int count) {
            return string.substring(0, start) + string.substring(start + count);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'TrimEnd'.
        //------------------------------------------------------------------------------------
        public static String trimEnd(String string, Character... charsToTrim) {
            if (string == null || charsToTrim == null)
                return string;

            int lengthToKeep = string.length();
            for (int index = string.length() - 1; index >= 0; index--) {
                boolean removeChar = false;
                if (charsToTrim.length == 0) {
                    if (Character.isWhitespace(string.charAt(index))) {
                        lengthToKeep = index;
                        removeChar = true;
                    }
                } else {
                    for (int trimCharIndex = 0; trimCharIndex < charsToTrim.length; trimCharIndex++) {
                        if (string.charAt(index) == charsToTrim[trimCharIndex]) {
                            lengthToKeep = index;
                            removeChar = true;
                            break;
                        }
                    }
                }
                if (!removeChar)
                    break;
            }
            return string.substring(0, lengthToKeep);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'TrimStart'.
        //------------------------------------------------------------------------------------
        public static String trimStart(String string, Character... charsToTrim) {
            if (string == null || charsToTrim == null)
                return string;

            int startingIndex = 0;
            for (int index = 0; index < string.length(); index++) {
                boolean removeChar = false;
                if (charsToTrim.length == 0) {
                    if (Character.isWhitespace(string.charAt(index))) {
                        startingIndex = index + 1;
                        removeChar = true;
                    }
                } else {
                    for (int trimCharIndex = 0; trimCharIndex < charsToTrim.length; trimCharIndex++) {
                        if (string.charAt(index) == charsToTrim[trimCharIndex]) {
                            startingIndex = index + 1;
                            removeChar = true;
                            break;
                        }
                    }
                }
                if (!removeChar)
                    break;
            }
            return string.substring(startingIndex);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'Trim' when arguments are used.
        //------------------------------------------------------------------------------------
        public static String trim(String string, Character... charsToTrim) {
            return trimEnd(trimStart(string, charsToTrim), charsToTrim);
        }

        //------------------------------------------------------------------------------------
        //	This method is used for string equality comparisons when the option
        //	'Use helper 'stringsEqual' method to handle null strings' is selected
        //	(The Java String 'equals' method can't be called on a null instance).
        //------------------------------------------------------------------------------------
        public static boolean stringsEqual(String s1, String s2) {
            if (s1 == null && s2 == null)
                return true;
            else
                return s1 != null && s1.equals(s2);
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'PadRight' (1 parameter version).
        //------------------------------------------------------------------------------------
        public static String padRight(String string, int totalWidth) {
            return padRight(string, totalWidth, ' ');
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'PadRight' (2 parameter version).
        //------------------------------------------------------------------------------------
        public static String padRight(String string, int totalWidth, char paddingChar) {
            StringBuilder sb = new StringBuilder(string);

            while (sb.length() < totalWidth) {
                sb.append(paddingChar);
            }

            return sb.toString();
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'PadLeft' (1 parameter version).
        //------------------------------------------------------------------------------------
        public static String padLeft(String string, int totalWidth) {
            return padLeft(string, totalWidth, ' ');
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'PadLeft' (2 parameter version).
        //------------------------------------------------------------------------------------
        public static String padLeft(String string, int totalWidth, char paddingChar) {
            StringBuilder sb = new StringBuilder("");

            while (sb.length() + string.length() < totalWidth) {
                sb.append(paddingChar);
            }

            sb.append(string);
            return sb.toString();
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string constructor which repeats a character.
        //------------------------------------------------------------------------------------
        public static String repeatChar(char charToRepeat, int count) {
            String newString = "";
            for (int i = 1; i <= count; i++) {
                newString += charToRepeat;
            }
            return newString;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'LastIndexOf' (char version).
        //------------------------------------------------------------------------------------
        public static int lastIndexOf(String string, char value, int startIndex, int count) {
            int leftMost = startIndex + 1 - count;
            int rightMost = startIndex + 1;
            String substring = string.substring(leftMost, rightMost);
            int lastIndexInSubstring = substring.lastIndexOf(value);
            if (lastIndexInSubstring < 0)
                return -1;
            else
                return lastIndexInSubstring + leftMost;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'LastIndexOf' (string version).
        //------------------------------------------------------------------------------------
        public static int lastIndexOf(String string, String value, int startIndex, int count) {
            int leftMost = startIndex + 1 - count;
            int rightMost = startIndex + 1;
            String substring = string.substring(leftMost, rightMost);
            int lastIndexInSubstring = substring.lastIndexOf(value);
            if (lastIndexInSubstring < 0)
                return -1;
            else
                return lastIndexInSubstring + leftMost;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'IndexOfAny' (1 parameter version).
        //------------------------------------------------------------------------------------
        public static int indexOfAny(String string, char[] anyOf) {
            int lowestIndex = -1;
            for (char c : anyOf) {
                int index = string.indexOf(c);
                if (index > -1) {
                    if (lowestIndex == -1 || index < lowestIndex) {
                        lowestIndex = index;

                        if (index == 0)
                            break;
                    }
                }
            }

            return lowestIndex;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'IndexOfAny' (2 parameter version).
        //------------------------------------------------------------------------------------
        public static int indexOfAny(String string, char[] anyOf, int startIndex) {
            int indexInSubstring = indexOfAny(string.substring(startIndex), anyOf);
            if (indexInSubstring == -1)
                return -1;
            else
                return indexInSubstring + startIndex;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'IndexOfAny' (3 parameter version).
        //------------------------------------------------------------------------------------
        public static int indexOfAny(String string, char[] anyOf, int startIndex, int count) {
            int endIndex = startIndex + count;
            int indexInSubstring = indexOfAny(string.substring(startIndex, endIndex), anyOf);
            if (indexInSubstring == -1)
                return -1;
            else
                return indexInSubstring + startIndex;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'LastIndexOfAny' (1 parameter version).
        //------------------------------------------------------------------------------------
        public static int lastIndexOfAny(String string, char[] anyOf) {
            int highestIndex = -1;
            for (char c : anyOf) {
                int index = string.lastIndexOf(c);
                if (index > highestIndex) {
                    highestIndex = index;

                    if (index == string.length() - 1)
                        break;
                }
            }

            return highestIndex;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'LastIndexOfAny' (2 parameter version).
        //------------------------------------------------------------------------------------
        public static int lastIndexOfAny(String string, char[] anyOf, int startIndex) {
            String substring = string.substring(0, startIndex + 1);
            int lastIndexInSubstring = lastIndexOfAny(substring, anyOf);
            if (lastIndexInSubstring < 0)
                return -1;
            else
                return lastIndexInSubstring;
        }

        //------------------------------------------------------------------------------------
        //	This method replaces the .NET string method 'LastIndexOfAny' (3 parameter version).
        //------------------------------------------------------------------------------------
        public static int lastIndexOfAny(String string, char[] anyOf, int startIndex, int count) {
            int leftMost = startIndex + 1 - count;
            int rightMost = startIndex + 1;
            String substring = string.substring(leftMost, rightMost);
            int lastIndexInSubstring = lastIndexOfAny(substring, anyOf);
            if (lastIndexInSubstring < 0)
                return -1;
            else
                return lastIndexInSubstring + leftMost;
        }

    }

}
