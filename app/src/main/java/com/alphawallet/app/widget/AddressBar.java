package com.alphawallet.app.widget;

import static com.alphawallet.app.util.KeyboardUtils.showKeyboard;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebBackForwardList;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.ui.widget.adapter.DappBrowserSuggestionsAdapter;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.util.KeyboardUtils;
import com.google.android.material.appbar.MaterialToolbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class AddressBar extends MaterialToolbar
{
    private final int ANIMATION_DURATION = 100;

    private AutoCompleteTextView urlTv;
    private DappBrowserSuggestionsAdapter adapter;
    private AddressBarListener listener;
    private ImageView btnClear;
    private View layoutNavigation;
    private ImageView back;
    private ImageView next;
    private ImageView home;

    @Nullable
    private Disposable disposable;
    private boolean focused;

    public AddressBar(Context context, AttributeSet attributeSet)
    {
        super(context, attributeSet);
        inflate(context, R.layout.layout_url_bar_full, this);

        initView();
    }

    public void setup(List<DApp> list, AddressBarListener listener)
    {
        adapter = new DappBrowserSuggestionsAdapter(
                getContext(),
                list,
                this::load
        );
        this.listener = listener;
        urlTv.setAdapter(null);

        urlTv.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO)
            {
                load(urlTv.getText().toString());
            }
            return false;
        });

        // Both these are required, the onFocus listener is required to respond to the first click.
        urlTv.setOnFocusChangeListener((v, hasFocus) -> {
            //see if we have focus flag
            if (hasFocus && focused) openURLInputView();
        });

        urlTv.setOnClickListener(v -> {
            openURLInputView();
        });

        urlTv.setShowSoftInputOnFocus(true);

        urlTv.setOnLongClickListener(v -> {
            urlTv.dismissDropDown();
            return false;
        });

        urlTv.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                adapter.setHighlighted(editable.toString());
            }
        });
    }

    private void load(String url)
    {
        listener.onLoad(url);
        expandCollapseView(layoutNavigation, true);
        leaveEditMode();
    }

    private void initView()
    {
        urlTv = findViewById(R.id.url_tv);
        home = findViewById(R.id.home);
        if (home != null) home.setOnClickListener(v -> {
            disableNavigationButtons();
            WebBackForwardList backForwardList = listener.onHomePagePressed();
            updateNavigationButtons(backForwardList);
        });

        btnClear = findViewById(R.id.clear_url);
        btnClear.setOnClickListener(v -> {
            clearAddressBar();
        });

        layoutNavigation = findViewById(R.id.layout_navigator);
        back = findViewById(R.id.back);
        back.setOnClickListener(v -> {
            disableNavigationButtons();
            WebBackForwardList backForwardList = listener.loadPrevious();
            updateNavigationButtons(backForwardList);
        });

        next = findViewById(R.id.next);
        next.setOnClickListener(v -> {
            disableNavigationButtons();
            WebBackForwardList backForwardList = listener.loadNext();
            updateNavigationButtons(backForwardList);
        });
    }

    private void clearAddressBar()
    {
        if (urlTv.getText().toString().isEmpty())
        {
            KeyboardUtils.hideKeyboard(urlTv);
            listener.onClear();
        }
        else
        {
            urlTv.getText().clear();
            openURLInputView();
            showKeyboard(urlTv); //ensure keyboard shows here so we can listen for it being cancelled
        }
    }

    private void openURLInputView()
    {
        urlTv.setAdapter(null);
        expandCollapseView(layoutNavigation, false);

        disposable = Observable.zip(
                        Observable.interval(600, TimeUnit.MILLISECONDS).take(1),
                        Observable.fromArray(btnClear), (interval, item) -> item)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::postBeginSearchSession);
    }

    private void postBeginSearchSession(@NotNull ImageView item)
    {
        urlTv.setAdapter(adapter);
        urlTv.showDropDown();
        if (item.getVisibility() == View.GONE)
        {
            expandCollapseView(item, true);
            showKeyboard(urlTv);
        }
    }

    private synchronized void expandCollapseView(@NotNull View view, boolean expandView)
    {
        //detect if view is expanded or collapsed
        boolean isViewExpanded = view.getVisibility() == View.VISIBLE;

        //Collapse view
        if (isViewExpanded && !expandView)
        {
            int finalWidth = view.getWidth();
            ValueAnimator valueAnimator = slideAnimator(finalWidth, 0, view);
            valueAnimator.addListener(new Animator.AnimatorListener()
            {
                @Override
                public void onAnimationStart(Animator animator)
                {

                }

                @Override
                public void onAnimationEnd(Animator animator)
                {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator)
                {

                }

                @Override
                public void onAnimationRepeat(Animator animator)
                {

                }
            });
            valueAnimator.start();
        }
        //Expand view
        else if (!isViewExpanded && expandView)
        {
            view.setVisibility(View.VISIBLE);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            view.measure(widthSpec, heightSpec);
            int width = view.getMeasuredWidth();
            ValueAnimator valueAnimator = slideAnimator(0, width, view);
            valueAnimator.start();
        }
    }

    @NotNull
    private ValueAnimator slideAnimator(int start, int end, final View view)
    {

        final ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(valueAnimator -> {
            // Update Height
            int value = (Integer) valueAnimator.getAnimatedValue();

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = value;
            view.setLayoutParams(layoutParams);
        });
        animator.setDuration(ANIMATION_DURATION);
        return animator;
    }

    public void removeSuggestion(DApp dApp)
    {
        adapter.removeSuggestion(dApp);
    }

    public void addSuggestion(DApp dapp)
    {
        adapter.addSuggestion(dapp);
    }

    public void shrinkSearchBar()
    {
        expandCollapseView(layoutNavigation, true);
        btnClear.setVisibility(View.GONE);
        urlTv.dismissDropDown();
    }

    public void destroy()
    {
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
    }

    public void clear()
    {
        if (urlTv != null)
            urlTv.getText().clear();
    }

    public void leaveEditMode()
    {
        if (urlTv != null)
        {
            urlTv.clearFocus();
            KeyboardUtils.hideKeyboard(urlTv);
            btnClear.setVisibility(GONE);
        }
        focused = true;
    }

    public void leaveFocus()
    {
        if (urlTv != null) urlTv.clearFocus();
        focused = false;
    }

    public void setUrl(String newUrl)
    {
        if (urlTv != null)
            urlTv.setText(newUrl);
    }

    public void updateNavigationButtons(WebBackForwardList backForwardList)
    {
        boolean isLast = backForwardList.getCurrentIndex() + 1 > backForwardList.getSize() - 1;
        if (isLast)
        {
            disableButton(next);
        }
        else
        {
            enableButton(next);
        }

        boolean isFirst = backForwardList.getCurrentIndex() == 0;
        if (isFirst)
        {
            disableButton(back);
        }
        else
        {
            enableButton(back);
        }
    }

    public boolean isOnHomePage()
    {
        return DappBrowserUtils.isDefaultDapp(urlTv.getText().toString());
    }

    public String getUrl()
    {
        return urlTv.getText().toString();
    }

    private void disableNavigationButtons()
    {
        disableButton(back);
        disableButton(next);
    }

    private void enableButton(ImageView button)
    {
        button.setEnabled(true);
        button.setAlpha(1.0f);
    }

    private void disableButton(ImageView button)
    {
        button.setEnabled(false);
        button.setAlpha(0.3f);
    }
}
