package com.pfa.pfaapp.httputils;

import android.content.Context;

import com.pfa.pfaapp.interfaces.HttpResponseCallback;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.pfa.pfaapp.httputils.ConfigHttpUtils.MAIN_MENU_POSTFIX;
import static com.pfa.pfaapp.utils.AppConst.SP_LOGIN_TYPE;

/**
 * HttpService->HttpUtils->SharedPrefUtils->AppUtils->CustomDialogs
 */
public class HttpService extends HttpUtils {
    //    Dev API
//    private static final String BASE_URL = "https://cell.pfa.gop.pk/dev/api/";
//    private static final String BASE_URL = "http://172.16.7.104/pfa/api/";
//    private static final String BASE_URL = "http://182.176.112.99/pfa/api/";

    //    Live APIhttps:
    private static final String BASE_URL = "https://cell.pfa.gop.pk/api/";
//    private static final String BASE_URL = "https://cellpfa.chimpstudio.co.uk/api/";

//    private static final String BASE_URL = "http://54.39.33.105/api/";


    public Context getContext() {
        return mContext;
    }

    public HttpService(Context mContext) {
        super(mContext);
    }

    public void authenticateUser(HashMap<String, String> httpParams, String type, HttpResponseCallback callback) {
        httpPost(BASE_URL + "account/login/" + type, httpParams, callback, true);
    }

    public void registerUser(HashMap<String, String> reqParams, boolean isUserVerified, HttpResponseCallback callback) {
        httpPost(isUserVerified ? BASE_URL + "account/registerAuth/1" : BASE_URL + "account/registerAuth", reqParams, callback, true);
    }

    public void forgetPin(HashMap<String, String> httpParams, HttpResponseCallback callback) {
        httpPost(BASE_URL + "account/forgetPin", httpParams, callback, true);
    }

    public void getLoginSettings(HttpResponseCallback callback) {
        httpGet(BASE_URL + "account/settings", new HashMap<String, String>(), callback, true);
    }

    public void getUserInfo(String userId, String fcmId, HttpResponseCallback callback, boolean showProgress) {
        httpGet(BASE_URL + "account/users/" + userId + "/" + fcmId + "/" + getSharedPrefValue(SP_LOGIN_TYPE, ""), new HashMap<String, String>(), callback, showProgress);
    }

    public void getMainMenu(HttpResponseCallback callback, String userID) {
        httpGet(BASE_URL + (MAIN_MENU_POSTFIX) + userID, new HashMap<String, String>(), callback, false);
    }


    public void getSideMenu(String userId, String userType, HttpResponseCallback callback) {
        httpGet(BASE_URL + "menu/" + userId + "/" + userType, new HashMap<String, String>(), callback, true);
    }

    public void getUserConfirmation(String userId, String pincode, HttpResponseCallback callback) {
        httpGet(BASE_URL + "account/user_authentication/" + userId + "/" + pincode, new HashMap<String, String>(), callback, true);
    }


    public void fetchConfigData(String suffix, HttpResponseCallback callback) {
        getListsData(suffix, new HashMap<String, String>(), callback, true);
    }

    public void getListsData(String suffix, HashMap<String, String> params, HttpResponseCallback callback, boolean showProgress) {
        httpGet(BASE_URL + suffix, params, callback, showProgress);
    }

    public void formSubmit(HashMap<String, String> httpParams, Map<String, File> fileParams, String suffix, HttpResponseCallback callback, boolean showProgress, String actionType) {
        if (fileParams != null && fileParams.size() > 0) {
            httpMultipartAPICall(BASE_URL + suffix, httpParams, fileParams, callback,showProgress);
        } else {
            if (actionType != null && actionType.equals("get")) {
                httpGet(BASE_URL + suffix, httpParams, callback, showProgress);
            } else {
                httpPost(BASE_URL + suffix, httpParams, callback, showProgress);
            }
        }
    }

    public void updateToken(HashMap<String, String> httpParams) {
        httpPost(BASE_URL + "account/tokenUpdate", httpParams, null, false);
    }

    public void sendEmergencyMessage(HashMap<String, String> httpParams, HttpResponseCallback callback) {
        httpPost(BASE_URL + "account/sendEmergencyMessage", httpParams, callback, false);
    }


    public void logout(HashMap<String, String> httpParams, HttpResponseCallback callback) {
        httpPost(BASE_URL + "account/tokenDelete", httpParams, callback, true);
    }

    public void logoutFromAll(HashMap<String, String> httpParams, HttpResponseCallback callback) {
        httpPost(BASE_URL + "account/logout_from_all_devices", httpParams, callback, true);
    }

    public void deleteDraftInspection(String inspectionID, HttpResponseCallback callback, boolean showProgress) {
        httpGet(BASE_URL + "inspections/download_inspection_show_back/" + inspectionID, new HashMap<String, String>(), callback, showProgress);
    }

    public void checkExistingUser(HashMap<String, String> params, HttpResponseCallback callback, boolean showProgress) {
        httpGet(BASE_URL + "account/check_existing_user", params, callback, showProgress);
    }

    public void setPinCode(HashMap<String, String> httpParams, String type, HttpResponseCallback callback) {
        httpPost(BASE_URL + "account/setpincode/" + type, httpParams, callback, true);
    }
}
