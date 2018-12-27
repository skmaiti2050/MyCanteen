package com.sujit.mycanteen.Utils;

import com.sujit.mycanteen.Retrofit.IMyCanteenAPI;
import com.sujit.mycanteen.Retrofit.RetrofitClient;

public class Common {
    private static final String BASE_URL = "http://192.168.1.6/MyCanteen/";

    public static IMyCanteenAPI getAPI()
    {
        return RetrofitClient.getClient(BASE_URL).create(IMyCanteenAPI.class);
    }
}
