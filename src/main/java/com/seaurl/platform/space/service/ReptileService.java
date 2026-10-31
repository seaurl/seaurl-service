package com.seaurl.platform.space.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public interface ReptileService {
    HashMap<String, Object> getFaviconFromUrl(String urlStr) throws IOException;

//    String getFaviconSuffixFromUrl(String urlStr) throws IOException;

    String getUrlTitle(String url) throws IOException;

    String getDomain(String urlStr)  throws IOException ;

    HashMap getUrlTitleAndDesc(String url) throws IOException ;
}
