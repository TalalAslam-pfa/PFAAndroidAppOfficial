package com.pfa.pfaapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pfa.pfaapp.customviews.PFASideMenuRB;
import com.pfa.pfaapp.fragments.CiTabbedFragment;
import com.pfa.pfaapp.fragments.DraftsFragment;
import com.pfa.pfaapp.fragments.LocalTabbedFragment;
import com.pfa.pfaapp.fragments.MenuFormFragment;
import com.pfa.pfaapp.fragments.MenuGridFragment;
import com.pfa.pfaapp.fragments.MenuListFragment;
import com.pfa.pfaapp.fragments.MenuMapFragment;
import com.pfa.pfaapp.fragments.ShareFragment;
import com.pfa.pfaapp.fragments.TabbedFragment;
import com.pfa.pfaapp.interfaces.HttpResponseCallback;
import com.pfa.pfaapp.interfaces.ListDataFetchedInterface;
import com.pfa.pfaapp.interfaces.RBClickCallback;
import com.pfa.pfaapp.interfaces.SendMessageCallback;
import com.pfa.pfaapp.models.PFAMenuInfo;
import com.pfa.pfaapp.models.UserInfo;
import com.pfa.pfaapp.utils.AppUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.pfa.pfaapp.utils.AppConst.EXTRA_ACTIVITY_TITLE;
import static com.pfa.pfaapp.utils.AppConst.EXTRA_FILTERS_DATA;
import static com.pfa.pfaapp.utils.AppConst.EXTRA_FORM_SECTION_LIST;
import static com.pfa.pfaapp.utils.AppConst.EXTRA_FP_ACTION;
import static com.pfa.pfaapp.utils.AppConst.FP_SIGNUP;
import static com.pfa.pfaapp.utils.AppConst.RC_ACTIVITY;
import static com.pfa.pfaapp.utils.AppConst.SP_DRAWER_MENU;
import static com.pfa.pfaapp.utils.AppConst.SP_IS_DELETE_DB_DELETED;
import static com.pfa.pfaapp.utils.AppConst.SP_IS_LOGED_IN;
import static com.pfa.pfaapp.utils.AppConst.SP_LOGIN_TYPE;
import static com.pfa.pfaapp.utils.AppConst.SP_SECURITY_CODE;
import static com.pfa.pfaapp.utils.AppConst.SP_STAFF_ID;
import static com.pfa.pfaapp.utils.AppConst.SP_USER_INFO;

public class PFADrawerActivity extends BaseActivity implements HttpResponseCallback, RBClickCallback {

    Bundle mySaveInstanceState;
    private String currentTab = "";
    private int lastClicked = -1;
    private boolean isHomeAlreadyAdded = false;
    private static final String KEY_FRAG_FIRST = "firstFrag";

    DrawerLayout drawer;
    RadioGroup sideMenuOptionsRG;
    List<PFAMenuInfo> pfaMenuInfos;

    TextView userNameInitTV, loggedUserNameTV, userAddressTV;

    List<Fragment> menuItemFragments = new ArrayList<>();
    UserInfo userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pfadrawer);

        drawer = findViewById(R.id.drawer_layout);

        setDownloadInspBtnClick();

        Log.d("onCreateActv" , "PFADrawerActivity");

        dbQueriesUtil.deleteExpiredInspections();
        if (sharedPrefUtils.getSharedPrefValue(SP_IS_DELETE_DB_DELETED, "") == null) {
            updateConfigData();
        }


        filterIV = findViewById(R.id.filterIV);
        searchFilterFL = findViewById(R.id.searchFilterFL);

        onClickPanicBtn((ImageButton) findViewById(R.id.panicAlertBtn));
        filterCountTV = findViewById(R.id.filterCountTV);
        if (filterCountTV != null)
            sharedPrefUtils.applyFont(filterCountTV, AppUtils.FONTS.HelveticaNeue);

        sideMenuOptionsRG = findViewById(R.id.sideMenuOptionsRG);
        loggedUserNameTV = findViewById(R.id.loggedUserNameTV);
        sharedPrefUtils.applyFont(loggedUserNameTV, AppUtils.FONTS.HelveticaNeueMedium);

        userNameInitTV = findViewById(R.id.userNameInitTV);
        sharedPrefUtils.applyFont(userNameInitTV, AppUtils.FONTS.HelveticaNeueMedium);

        userAddressTV = findViewById(R.id.userAddressTV);
        sharedPrefUtils.applyFont(userAddressTV, AppUtils.FONTS.HelveticaNeue);

        if (sharedPrefUtils.getSharedPrefValue(SP_USER_INFO, "") == null) {
            fetchUserInfo(new HttpResponseCallback() {
                @Override
                public void onCompleteHttpResponse(JSONObject response, String requestUrl) {
                    PFADrawerActivity.this.onCompleteHttpResponse(response, requestUrl);
                }
            }, false);
            updateConfigData();
        } else {
            getSideMenu();
        }

        sharedPrefUtils.clearAllNotifications(-1);
        setFilterIVClick();

//        register broadcast receiver for showing the help activity [on locked screen]
        registerScreenReceiver();



//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        getConfirmation();
//                    }
//                });
//            }
//        }, 1500);

        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                        getConfirmation();
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 60000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED) {
                //permission not enabled, request it
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                //show popup to request permissions
                requestPermissions(permission, 11);
            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED) {
                //permission not enabled, request it
                String[] permission = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                //show popup to request permissions
                requestPermissions(permission, 10);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager())
            {
                Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION );
                startActivity(permissionIntent);
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 11) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();

                String permission_status ="Permission_granted";
                 sharedPrefUtils.savePermissionStatus(permission_status);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }

        }else if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "gallery permission granted", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(this, "gallery permission denied", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Error in permissions", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void getConfirmation() {
        String pincode = "";
        String userId = "";
        if (sharedPrefUtils.getSharedPrefValue(SP_IS_LOGED_IN, "") != null) {
            userId = sharedPrefUtils.getSharedPrefValue(SP_STAFF_ID, "");
        }
        pincode = sharedPrefUtils.getSharedPrefValue(SP_SECURITY_CODE, "");
        httpService.getUserConfirmation(userId, pincode, new HttpResponseCallback() {
            @Override
            public void onCompleteHttpResponse(JSONObject response, String requestUrl) {
                if (response!= null)
                {
                    try {
                        String status = response.getString("status");
                        if (status == "false"){
                            if (sharedPrefUtils.getSharedPrefValue(SP_IS_LOGED_IN, "") != null) {
                                sharedPrefUtils.logoutFromApp(httpService);
                                Toast.makeText(PFADrawerActivity.this, "Unauthentic User", Toast.LENGTH_SHORT).show();

                            } else {
                                sharedPrefUtils.startNewActivity(LoginActivity.class, null, false);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void setFilterIVClick() {
        searchFilterFL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MenuListFragment menuListFragment = null;
                if (menuItemFragments.get(lastClicked) instanceof TabbedFragment) {
                    Fragment fragment = ((TabbedFragment) menuItemFragments.get(lastClicked)).getCurrentFragment();

                    if (fragment instanceof MenuListFragment) {
                        menuListFragment = (MenuListFragment) fragment;
                    }
                } else if (menuItemFragments.get(lastClicked) instanceof CiTabbedFragment) {
                    Fragment fragment = ((CiTabbedFragment) menuItemFragments.get(lastClicked)).getCurrentFragment();

                    if (fragment instanceof MenuListFragment) {
                        menuListFragment = (MenuListFragment) fragment;
                    }
                } else if (menuItemFragments.get(lastClicked) instanceof MenuListFragment) {
                    menuListFragment = (MenuListFragment) menuItemFragments.get(lastClicked);
                }

                if (menuListFragment != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(EXTRA_ACTIVITY_TITLE, "Select List Filters");
                    if (filterCountTV.getText().toString().isEmpty()) {
                        if (menuListFragment.formFilteredData != null && menuListFragment.formFilteredData.size() > 0)
                            menuListFragment.formFilteredData.clear();
                    }
                    bundle.putSerializable(EXTRA_FILTERS_DATA, menuListFragment.formFilteredData);
                    bundle.putSerializable(EXTRA_FORM_SECTION_LIST, (Serializable) menuListFragment.formSectionInfos);

                    sharedPrefUtils.startActivityForResult(PFADrawerActivity.this, PFAFiltersActivity.class, bundle, RC_ACTIVITY);
                }
            }
        });


    }

    private void backPressedAction() {
        downloadInspImgBtn.setVisibility(View.GONE);

        lastClicked = 0;
        currentTab = pfaMenuInfos.get(lastClicked).getMenuItemName();

        actionOnViewChange();

        if (sideMenuOptionsRG != null) {
            ((RadioButton) sideMenuOptionsRG.getChildAt(lastClicked)).setChecked(true);
        }

        setTitle(currentTab, false);
    }

    @Override
    public void onBackPressed() {
        if (pfaMenuInfos == null || pfaMenuInfos.size() == 0) {
            finish();
            return;
        }
        backPressedAction();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("imagePath" , "onActivityResult = " + "PFADrawerActivity");

        if (lastClicked >= 0) {
            menuItemFragments.get(lastClicked).onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getSideMenu() {
        userInfo = sharedPrefUtils.getUserInfo();
        if (userInfo != null) {
            String fullNameStr = String.format(Locale.getDefault(), "%s %s", userInfo.getFirstname(), userInfo.getLastname());
            loggedUserNameTV.setText(sharedPrefUtils.capitalize(fullNameStr));
            userNameInitTV.setText(("" + userInfo.getFirstname().charAt(0)).toUpperCase());
            StringBuilder addressStr = new StringBuilder();
            if (userInfo.getAddress_obj().getSubtown_name() != null && (!userInfo.getAddress_obj().getSubtown_name().isEmpty())) {
                addressStr.append(userInfo.getAddress_obj().getSubtown_name());
            }
            if (userInfo.getAddress_obj().getTown_name() != null && (!userInfo.getAddress_obj().getTown_name().isEmpty())) {
                if (!addressStr.toString().isEmpty()) {
                    addressStr.append(",");
                }
                addressStr.append(userInfo.getAddress_obj().getTown_name());
            }

            if (userInfo.getAddress_obj().getDistrict_name() != null && (!userInfo.getAddress_obj().getDistrict_name().isEmpty())) {
                if (!addressStr.toString().isEmpty()) {
                    addressStr.append(",");
                }
                addressStr.append(userInfo.getAddress_obj().getDistrict_name());
            }

            userAddressTV.setText(addressStr.toString());
        }



        if (sharedPrefUtils.getDrawerMenu() == null)
            httpService.getSideMenu("" + sharedPrefUtils.getSharedPrefValue(SP_STAFF_ID, ""), sharedPrefUtils.getSharedPrefValue(SP_LOGIN_TYPE, ""), this);
        else
            populateSideMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lastClicked >= 0) {
            menuItemFragments.get(lastClicked).onResume();
        }
        startLocation();
    }

    private void hideNoDataImg() {

        ///////////
        try {
            downloadInspImgBtn.setVisibility(View.GONE);
            getSupportFragmentManager().getFragments();
            if (lastClicked >= getSupportFragmentManager().getFragments().size()) {
                return;
            }

            if (getSupportFragmentManager().getFragments().get(lastClicked) instanceof MenuListFragment) {
                if (lastClicked < (getSupportFragmentManager().getFragments().size())) {
                    View view = getSupportFragmentManager().getFragments().get(lastClicked).getView().findViewById(R.id.sorry_iv);
                    if (view != null)
                        view.setVisibility(View.GONE);
                }
                if (lastClicked == 0)
                    ((MenuListFragment) getSupportFragmentManager().getFragments().get(lastClicked)).onRefreshListener.onRefresh();

            } else if (getSupportFragmentManager().getFragments().get(lastClicked) instanceof TabbedFragment) {
                ((TabbedFragment) getSupportFragmentManager().getFragments().get(lastClicked)).refreshData();
            } else if (getSupportFragmentManager().getFragments().get(lastClicked) instanceof CiTabbedFragment) {
                ((CiTabbedFragment) getSupportFragmentManager().getFragments().get(lastClicked)).refreshData();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //////////
    }

    @Override
    public void onCompleteHttpResponse(JSONObject response, String requestUrl) {

        if (response != null) {
            if (response.optBoolean("status")) {

                if (requestUrl.contains("/account/users/")) {
                    sharedPrefUtils.saveSharedPrefValue(SP_USER_INFO, response.optJSONObject("data").toString());

                    getSideMenu();

                } else if (requestUrl.contains("/api/menu/")) {
                    try {
                        JSONObject jsonObject = response.getJSONObject("data");

                        JSONArray formJSONArray = jsonObject.getJSONArray("menus");

                        sharedPrefUtils.saveSharedPrefValue(SP_DRAWER_MENU, formJSONArray.toString());
                        populateSideMenu();


                    } catch (JSONException e) {
                        sharedPrefUtils.printStackTrace(e);
                    }
                }
            }
        }
    }

    private void populateSideMenu() {
        pfaMenuInfos = sharedPrefUtils.getDrawerMenu();

        new PFASideMenuRB(PFADrawerActivity.this, sideMenuOptionsRG, pfaMenuInfos, PFADrawerActivity.this);

        if (pfaMenuInfos != null && pfaMenuInfos.size() > 0) {
            for (final PFAMenuInfo pfaMenuInfo : pfaMenuInfos) {

                Fragment menuItemFragment;


                switch (pfaMenuInfo.getMenuType()) {
                    case "list":
                        menuItemFragment = MenuListFragment.newInstance(pfaMenuInfo, true, true, true, null);
                        ((MenuListFragment) menuItemFragment).setFetchDataInterface(new ListDataFetchedInterface() {
                            @Override
                            public void listDataFetched() {
                                hideShowFilters();
                            }
                        });
                        break;
                    case "menu":
                        menuItemFragment = TabbedFragment.newInstance(pfaMenuInfo, true);
                        break;
                    case "ci_menu":
                        menuItemFragment = CiTabbedFragment.newInstance(pfaMenuInfo, true);
                        break;
                    case "localMenu":
                        menuItemFragment = LocalTabbedFragment.newInstance(pfaMenuInfo, true);
                        break;
                    case "googlemap":
                        menuItemFragment = MenuMapFragment.newInstance(pfaMenuInfo, null);
                        break;
                    case "dashboard":
                    case "grid":
                        menuItemFragment = MenuGridFragment.newInstance(pfaMenuInfo);
                        break;
                    case "logout":
//                        AlertDialog.Builder builder = new AlertDialog.Builder(PFADrawerActivity.this);
//                        builder.setTitle("Log out");
//
//                        String[] options = {"Logout","Logout from all devices"};
//                        builder.setItems(options, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                switch (which) {
//                                    case 0:
//                                        if (sharedPrefUtils.getSharedPrefValue(SP_IS_LOGED_IN, "") != null) {
//                                            sharedPrefUtils.logoutFromApp(httpService);
//                                        } else {
//                                            sharedPrefUtils.startNewActivity(LoginActivity.class, null, false);
//                                        }
//                                        break;
//                                    case 1:
//
//                                        if (sharedPrefUtils.getSharedPrefValue(SP_IS_LOGED_IN, "") != null) {
//                                            sharedPrefUtils.logoutFromAllDevices(httpService);
//                                        } else {
//                                            sharedPrefUtils.startNewActivity(LoginActivity.class, null, false);
//                                        }
//                                        break;
//                                }
//                            }
//                        });
//
//                        AlertDialog dialog = builder.create();
//                        dialog.show();
//                        break;

                    case "fingerPrint":
                        menuItemFragment = new Fragment();
                        break;
                    case "share":
                        menuItemFragment = ShareFragment.newInstance(pfaMenuInfo);
                        break;

                    case "draft":
                        menuItemFragment = DraftsFragment.newInstance(pfaMenuInfo, new SendMessageCallback() {
                            @Override
                            public void sendMsg(String message) {
                            }
                        });
                        break;
                    default:
                        menuItemFragment = MenuFormFragment.newInstance(pfaMenuInfo, null);
                        break;
                }

                if (menuItemFragment != null)
                    menuItemFragments.add(menuItemFragment);
            }
            lastClicked = 0;

            addFragment(menuItemFragments.get(0), true, pfaMenuInfos.get(0).getMenuItemName());
        }
    }

    @Override
    public void onClickRB(View view) {

        if (view.getTag().toString().equalsIgnoreCase("logout")) {
//            sharedPrefUtils.logoutFromApp(httpService);
            AlertDialog.Builder builder = new AlertDialog.Builder(PFADrawerActivity.this);
//                builder.setTitle("Log out");

            String[] options = {"Log Out","Log Out from All Devices"};
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            if (sharedPrefUtils.getSharedPrefValue(SP_IS_LOGED_IN, "") != null) {
                                sharedPrefUtils.logoutFromApp(httpService);
                            } else {
                                sharedPrefUtils.startNewActivity(LoginActivity.class, null, false);
                            }
                            break;
                        case 1:

                            if (sharedPrefUtils.getSharedPrefValue(SP_IS_LOGED_IN, "") != null) {
                                sharedPrefUtils.logoutFromAllDevices(httpService);
                            } else {
                                sharedPrefUtils.startNewActivity(LoginActivity.class, null, false);
                            }
                            break;
                    }
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }

        if (view.getTag().toString().equalsIgnoreCase("Add Fingerprint")) {
            drawer.closeDrawer(GravityCompat.START);

            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_FP_ACTION, FP_SIGNUP);
            sharedPrefUtils.startNewActivity(FPrintActivity.class, bundle, false);
            return;
        }

        int id = view.getId();



        if (lastClicked == id) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }

        addFragment(menuItemFragments.get(id), id == 0, pfaMenuInfos.get(id).getMenuItemName());
        if (drawer != null)
            drawer.closeDrawer(GravityCompat.START);

        lastClicked = id;

        if (lastClicked == 0)
            hideNoDataImg();

        removeFilter();
        setTitle(pfaMenuInfos.get(id).getMenuItemName(), false);
        hideShowFilters();
    }

    private void hideShowFilters() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                searchFilterFL.setVisibility(View.GONE);
                if (menuItemFragments.get(lastClicked) instanceof MenuListFragment) {
                    if (((MenuListFragment) menuItemFragments.get(lastClicked)).showFilter) {
                        searchFilterFL.setVisibility(View.VISIBLE);
                    }
                }
            }
        }, 200);
    }

    private void actionOnViewChange() {
        if (drawer != null)
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();

        if (backStackCount == 1) {
            sharedPrefUtils.showExitDialog();
            return;
        }

        if (backStackCount > 1) {
            hideNoDataImg();
            getSupportFragmentManager().popBackStack();
            lastClicked = 0;
        }
    }

    public void addFragment(Fragment frag, boolean isHome, String fragmentTitleStr) {

        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            actionOnViewChange();
        }

        if (mySaveInstanceState == null) {
            if (isHome) {
                currentTab = fragmentTitleStr;
                if (isHomeAlreadyAdded)
                    return;
                else {
                    isHomeAlreadyAdded = true;
                }
            }

            if (isHome || (!fragmentTitleStr.equals(currentTab))) {
                currentTab = fragmentTitleStr;
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setReorderingAllowed(true);
                transaction.add(getFrameLayoutId(isHome), frag);
                transaction.addToBackStack(getSupportFragmentManager().getBackStackEntryCount() == 0 ? KEY_FRAG_FIRST : currentTab).commit();
            }
        }

        setTitle(currentTab, false);
    }

    public int getFrameLayoutId(boolean isHomeScreen) {
        if (isHomeScreen) {
            return R.id.mainContentFL;
        } else {
            return R.id.main_screen_fragmentsFL;
        }
    }

    public void onClickMenuImgBtn(View view) {
        hideKeyBoard();
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocation();
    }

    public void onClickNotifMsgTV(View view) {
        sharedPrefUtils.startNewActivity(NotificationActivity.class, null, false);
    }
}
