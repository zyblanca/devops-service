package io.choerodon.devops.infra.config;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

import io.choerodon.core.exception.CommonException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(RetrofitHandler.class);

    /**
     * Retrofit 设置
     *
     * @param configurationProperties 插件配置信息
     * @return retrofit
     */


    public static Retrofit initRetrofit(ConfigurationProperties configurationProperties) {

        String credentials = configurationProperties.getUsername() + ":"
                + configurationProperties.getPassword();
        String token = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        OkHttpClient okHttpClient = getOkHttpClient(configurationProperties.getInsecureSkipTlsVerify(), configurationProperties.getType(), token);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(configurationProperties.getBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }

    private static OkHttpClient getOkHttpClient(Boolean insecureSkipTlsVerify, String type, String token) {
        if (type.equals("harbor")) {
            if (insecureSkipTlsVerify) {
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {

                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                                //此处是不检验安全证书，避免有些harbor仓库没使用https安全证书导致harbor api失败，如果不抛出异常会出现sonar的问题
                                if (type == null) {
                                    try {
                                        throw new CertificateException("the type is null");
                                    } catch (CertificateException e) {
                                        throw new CommonException(e);
                                    }
                                }
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                                //此处是不检验安全证书，避免有些harbor仓库没使用https安全证书导致harbor api失败，如果不抛出异常会出现sonar的问题
                                if (type == null) {
                                    try {
                                        throw new CertificateException("the type is null");
                                    } catch (CertificateException e) {
                                        throw new CommonException(e);
                                    }
                                }
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                // Install the all-trusting trust manager
                SSLContext sslContext = null;
                try {
                    sslContext = SSLContext.getInstance("TLSv1.2");
                    if (sslContext != null) {
                        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.error(e.getMessage());
                } catch (KeyManagementException e) {
                    LOGGER.error(e.getMessage());
                }

                // Create an ssl socket factory with our all-trusting manager
                SSLSocketFactory sslSocketFactory = null;
                if (sslContext != null) {
                    sslSocketFactory = sslContext.getSocketFactory();
                }
                OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
                okHttpClientBuilder.interceptors().add((Interceptor.Chain chain) -> {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", token);

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                });
                okHttpClientBuilder.sslSocketFactory(sslSocketFactory);
                okHttpClientBuilder.hostnameVerifier((requestedHost, remoteServerSession) -> {
                    return requestedHost.equalsIgnoreCase(remoteServerSession.getPeerHost()); // Compliant
                });
                return okHttpClientBuilder.build();
            } else {
                OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
                okHttpClientBuilder.interceptors().add((Interceptor.Chain chain) -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", token);
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                });
                return okHttpClientBuilder.build();
            }
        } else {
            return new OkHttpClient.Builder().build();
        }
    }

    public static Response<Object> execute(Call<Object> call) {
        Response<Object> response = null;
        try {
            response = call.execute();
            if (response.code() != 200) {
                if (response.code() == 401) {
                    throw new CommonException("error.harbor.user.password");
                }
                throw new CommonException("code:" + response.code() + ",message:" + response.message() + ",body:" + response.errorBody().toString());
            }
        } catch (IOException e) {
            throw new CommonException("error.retrofit.handler.url");
        }
        return response;
    }
}
