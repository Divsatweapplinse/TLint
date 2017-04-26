package com.gzsll.hupu.ui.splash;

import android.text.TextUtils;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import com.gzsll.hupu.R;
import com.gzsll.hupu.ui.BaseActivity;
import com.gzsll.hupu.ui.main.MainActivity;
import com.gzsll.hupu.ui.messagelist.MessageActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by sll on 2016/3/11.
 */
public class SplashActivity extends BaseActivity implements SplashContract.View {

    public static final String ACTION_NOTIFICATION_MESSAGE =
            "com.gzsll.hupu.ACTION_NOTIFICATION_MESSAGE";

    @BindView(R.id.splash)
    FrameLayout splash;

    @Inject
    SplashPresenter mPresenter;

    @Override
    public int initContentView() {
        return R.layout.activity_splash;
    }

    @Override
    public void initInjector() {
        DaggerSplashComponent.builder()
                .applicationComponent(getApplicationComponent())
                .activityModule(getActivityModule())
                .splashModule(new SplashModule(this))
                .build()
                .inject(this);
    }

    @Override
    public void initUiAndListener() {
        ButterKnife.bind(this);
        mPresenter.attachView(this);
        mPresenter.initUmeng();
        mPresenter.initHuPuSign();
    }

    @Override
    protected boolean isApplyStatusBarTranslucency() {
        return true;
    }

    @Override
    protected boolean isApplyStatusBarColor() {
        return false;
    }

    @Override
    public void showMainUi() {
        AlphaAnimation aa = new AlphaAnimation(0.7f, 1.0f);
        aa.setDuration(2000);
        splash.startAnimation(aa);
        aa.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                MainActivity.startActivity(SplashActivity.this);
                String action = getIntent().getAction();
                if (TextUtils.equals(action, ACTION_NOTIFICATION_MESSAGE)) {
                    MessageActivity.startActivity(SplashActivity.this);
                }
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
