package com.piddlepops.awardl;

import com.piddlepops.awardl.reswords.WordsResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APIInterface {
    public static String BASE_URL = "http://192.168.128.147:5000/";
    @POST("postResult")
    Call<ResponseBody> postResult(@Body PostResultModel postResultModel);

    @GET("getWords")
    Call<WordsResponse> getWords();
}