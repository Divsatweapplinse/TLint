package com.gzsll.hupu.ui.login;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gzsll.hupu.Constants;
import com.gzsll.hupu.api.game.GameApi;
import com.gzsll.hupu.bean.LoginData;
import com.gzsll.hupu.bean.LoginResult;
import com.gzsll.hupu.bean.UserData;
import com.gzsll.hupu.bean.UserResult;
import com.gzsll.hupu.components.storage.UserStorage;
import com.gzsll.hupu.db.User;
import com.gzsll.hupu.db.UserDao;
import com.gzsll.hupu.injector.PerActivity;
import com.gzsll.hupu.otto.LoginSuccessEvent;
import com.gzsll.hupu.util.SecurityUtils;
import com.gzsll.hupu.util.ToastUtils;
import com.squareup.otto.Bus;

import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by sll on 2016/3/10.
 */
@PerActivity
public class LoginPresenter implements LoginContract.Presenter {
    Logger logger = Logger.getLogger(LoginPresenter.class.getSimpleName());

    private GameApi mGameApi;
    private Bus mBus;
    private UserDao mUserDao;
    private UserStorage mUserStorage;

    private LoginContract.View mLoginView;
    private User user = new User();
    private Subscription mSubscription;

    @Inject
    public LoginPresenter(GameApi gameApi, Bus bus, UserDao userDao, UserStorage userStorage) {
        mGameApi = gameApi;
        mBus = bus;
        mUserDao = userDao;
        mUserStorage = userStorage;
    }

    @Override
    public void login(final String userName, final String passWord) {
        if (TextUtils.isEmpty(userName)) {
            mLoginView.showUserNameError("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(passWord)) {
            mLoginView.showPassWordError("请输入密码");
            return;
        }
        mLoginView.showLoading();
        mSubscription = mGameApi.login(userName, SecurityUtils.getMD5(passWord))
                .flatMap(new Func1<LoginData, Observable<UserData>>() {
                    @Override
                    public Observable<UserData> call(LoginData loginData) {
                        if (loginData != null && loginData.is_login == 1) {
                            LoginResult data = loginData.result;
                            String cookie = "";
                            try {
                                cookie = URLDecoder.decode(Constants.Cookie, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            logger.debug("cookie:" + cookie);
                            String uid = cookie.split("\\|")[0];
                            logger.debug("uid:" + uid);
                            user.setUid(uid);
                            user.setToken(data.token);
                            user.setCookie(cookie);
                            user.setUserName(data.username);
                            return mGameApi.getUserInfo(uid);
                        }
                        return null;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<UserData>() {
                    @Override
                    public void call(UserData userData) {
                        if (userData != null && userData.result != null) {
                            UserResult data = userData.result;
                            user.setIcon(data.header);
                            user.setThreadUrl(data.bbs_msg_url);
                            user.setPostUrl(data.bbs_post_url);
                            user.setRegisterTime(data.reg_time_str);
                            user.setSchool(data.school);
                            user.setSex(data.gender);
                            user.setLocation(data.location_str);
                            mUserStorage.login(user);
                            insertOrUpdateUser(user);
                            ToastUtils.showToast("登录成功");
                            mBus.post(new LoginSuccessEvent());
                            mLoginView.loginSuccess();
                        } else {
                            mLoginView.hideLoading();
                            ToastUtils.showToast("登录失败，请检查您的网络");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                        mLoginView.hideLoading();
                        ToastUtils.showToast("登录失败，请检查您的网络");
                    }
                });
    }

    private void insertOrUpdateUser(User user) {
        List<User> users =
                mUserDao.queryBuilder().where(UserDao.Properties.Uid.eq(user.getUid())).list();
        if (!users.isEmpty()) {
            user.setId(users.get(0).getId());
        }
        mUserDao.insertOrReplace(user);
    }

    @Override
    public void attachView(@NonNull LoginContract.View view) {
        mLoginView = view;
    }

    @Override
    public void detachView() {
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mLoginView = null;
    }
}
