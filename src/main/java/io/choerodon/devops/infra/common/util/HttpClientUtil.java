package io.choerodon.devops.infra.common.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import io.choerodon.core.exception.CommonException;

public class HttpClientUtil {

    HttpClientUtil() {
    }

    /**
     * 下载 tgz
     *
     * @param getUrl  tgz路径
     * @param fileUrl 目标路径
     */
    public static void getTgz(String getUrl, String fileUrl) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(getUrl);
            CloseableHttpResponse response1 = httpclient.execute(httpGet);
            try (FileOutputStream fos = new FileOutputStream(fileUrl)) {
                InputStream is = response1.getEntity().getContent();
                byte[] buffer = new byte[4096];
                int r = 0;
                while ((r = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, r);
                }
                is.close();
            }
        } catch (IOException e) {
            throw new CommonException(e.getMessage());
        }
    }


    public static Integer getSonar(String sonarUrl) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(sonarUrl);
            CloseableHttpResponse response1 = httpclient.execute(httpGet);
            return response1.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new CommonException(e.getMessage());
        }
    }

}
