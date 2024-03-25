package com.alphawallet.app.widget;

import static androidx.core.content.ContextCompat.getColorStateList;
import static com.alphawallet.app.util.Utils.ALPHAWALLET_REPO_NAME;
import static com.alphawallet.app.util.Utils.TRUST_ICON_REPO_BASE;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.TokenIconViewModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import org.jetbrains.annotations.NotNull;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TokenIcon extends ConstraintLayout
{
    private final ImageView icon;
    private final ImageView iconSecondary;
    private final TextView textIcon;
    private final ImageView statusIcon;
    private final ImageView circle;
    private final ProgressBar pendingProgress;
    private final ImageView statusBackground;
    private final ImageView chainIcon;
    private final ImageView chainIconBackground;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final boolean squareToken;
    private TokensAdapterCallback tokensAdapterCallback;
    private volatile Token token;
    private final TokenIconViewModel viewModel;
    @Nullable
    private Disposable disposable;
    private String tokenName;
    private StatusType currentStatus;
    private Request currentRq;
    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private final RequestListener<Drawable> requestListener = new RequestListener<>()
    {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource)
        {
            loadFromAltRepo(model);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource)
        {
            if (model == null) return false;
            if (token != null)
            {
                viewModel.storeImageUrl(token.tokenInfo.chainId, token.tokenInfo.address, model.toString());
            }

            handler.post(() -> {
                textIcon.setVisibility(View.GONE);
                iconSecondary.setVisibility(View.VISIBLE);
                iconSecondary.setImageDrawable(resource);
            });

            return false;
        }
    };

    public TokenIcon(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        squareToken = getViewId(context, attrs);

        inflate(context, squareToken ? R.layout.item_token_icon_square : R.layout.item_token_icon, this);

        icon = findViewById(R.id.icon);
        iconSecondary = findViewById(R.id.icon_secondary);
        textIcon = findViewById(R.id.text_icon);
        statusIcon = findViewById(R.id.status_icon);
        circle = findViewById(R.id.circle);
        pendingProgress = findViewById(R.id.pending_progress);
        statusBackground = findViewById(R.id.status_icon_background);
        statusIcon.setVisibility(isInEditMode() ? View.VISIBLE : View.GONE);
        currentStatus = StatusType.NONE;
        chainIcon = findViewById(R.id.status_chain_icon);
        chainIconBackground = findViewById(R.id.chain_icon_background);
        viewModel = new ViewModelProvider((ViewModelStoreOwner) getActivity()).get(TokenIconViewModel.class);

        bindViews();
    }

    private boolean getViewId(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
            attrs,
            R.styleable.TokenIcon,
            0, 0
        );

        boolean sq;

        try
        {
            sq = a.getBoolean(R.styleable.TokenIcon_square, false);
        }
        finally
        {
            a.recycle();
        }

        return sq;
    }

    private void bindViews()
    {
        View layout = findViewById(R.id.view_container);
        layout.setOnClickListener(this::performTokenClick);
    }

    public void clearLoad()
    {
        iconSecondary.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(null);
        if (currentRq != null && currentRq.isRunning())
        {
            currentRq.pause();
            currentRq.clear();
            currentRq = null;
            handler.removeCallbacksAndMessages(null);
            token = null;
        }

        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
        }
    }

    /**
     * This method is necessary to call from the binder to show information correctly.
     *
     * @param token           Token object
     */
    public void bindData(Token token)
    {
        this.token = token;
        clearLoad();

        if (token.isEthereum())
        {
            bindData(token.tokenInfo.chainId);
            return;
        }

        if (token.group == TokenGroup.SPAM)
        {
            bindSpam(token);
        }
        else
        {
            this.tokenName = viewModel.getTokenName(token);
            String mainIcon = viewModel.getTokenIcon(token);
            bind(token, mainIcon);
        }
    }

    public void bindData(long chainId)
    {
        clearLoad();
        loadImageFromResource(EthereumNetworkRepository.getChainLogo(chainId));
    }

    private void bind(Token token, String iconUrl)
    {
        bindCommon(token);
        displayTokenIcon(iconUrl);
    }

    private void bindSpam(Token token)
    {
        bindCommon(token);
        setSpam();
    }

    private void bindCommon(Token token)
    {
        this.handler.removeCallbacks(null);
        this.token = token;

        iconSecondary.setVisibility(View.INVISIBLE);
        statusBackground.setVisibility(View.GONE);
        chainIconBackground.setVisibility(View.GONE);
        chainIcon.setVisibility(View.GONE);
    }

    public void setChainIcon(long chainId)
    {
        chainIconBackground.setVisibility(View.VISIBLE);
        chainIcon.setVisibility(View.VISIBLE);
        chainIcon.setImageResource(EthereumNetworkRepository.getSmallChainLogo(chainId));
    }

    public void setSpam()
    {
        textIcon.setVisibility(View.GONE);
        icon.setImageResource(R.drawable.ic_spam_token);
        circle.setVisibility(View.GONE);
    }

    private void setupDefaultIcon()
    {
        if (token.isEthereum() || EthereumNetworkRepository.getChainOverrideAddress(token.tokenInfo.chainId).equalsIgnoreCase(token.getAddress()))
        {
            loadImageFromResource(EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId));
        }
        else
        {
            setupTextIcon(token);
        }
    }

    /**
     * Try to fetch Token Icon from the Token URL.
     */
    private void displayTokenIcon(String iconUrl)
    {
        setupDefaultIcon();

        final RequestOptions optionalCircleCrop = squareToken || iconUrl.startsWith(ALPHAWALLET_REPO_NAME) ? new RequestOptions() : new RequestOptions().circleCrop();

        currentRq = Glide.with(this)
                .load(iconUrl)
                .placeholder(R.drawable.ic_token_eth)
                .apply(optionalCircleCrop)
                .listener(requestListener)
                .into(new DrawableImageViewTarget(icon)).getRequest();
    }

    public void setStatusIcon(StatusType type)
    {
        boolean requireAnimation = statusIcon.getVisibility() == View.VISIBLE && type != currentStatus;
        statusIcon.setVisibility(View.VISIBLE);
        pendingProgress.setVisibility(View.GONE);
        switch (type)
        {
            case PENDING:
                pendingProgress.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                statusIcon.setImageResource(R.drawable.ic_rejected_small);
                break;
            case REJECTED:
                statusIcon.setImageResource(R.drawable.ic_transaction_rejected);
                break;
            case CONSTRUCTOR:
                statusIcon.setImageResource(EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId));
                statusBackground.setVisibility(View.VISIBLE);
                break;
            default:
                statusIcon.setVisibility(View.GONE);
                break;
        }

        currentStatus = type;

        if (requireAnimation)
        {
            statusIcon.setAlpha(0.0f);
            statusIcon.animate().alpha(1.0f).setDuration(500);
        }
    }

    /**
     * Attempt to load the icon from the Database icon or TW icon repo
     */
    private void loadFromAltRepo(Object model)
    {
        //check how to load from alt repo or ignore
        String checkUrl = model != null ? model.toString() : null;
        if (!Utils.stillAvailable(getContext()) || token == null || TextUtils.isEmpty(checkUrl))
        {
            return;
        }

        //check heuristic:
        //1. If it's the AW iconassets repo then check using contract URI
        //2. If it's not Trust repo URL then check using Trust repo
        //3. If it is the Trust repo then stop

        boolean useContractURI;

        if (checkUrl.startsWith(ALPHAWALLET_REPO_NAME))
        {
            useContractURI = true;
        }
        else if (!checkUrl.startsWith(TRUST_ICON_REPO_BASE))
        {
            useContractURI = false;
        }
        else
        {
            return; //we checked Trust repo, this is the final check
        }

        disposable = viewModel.getIconFallback(token, useContractURI)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadImageAlt);
    }

    private void loadImageAlt(String fileUri)
    {
        final RequestOptions optionalCircleCrop = squareToken ? new RequestOptions() : new RequestOptions().circleCrop();

        currentRq = Glide.with(this)
                .load(fileUri)
                .apply(optionalCircleCrop)
                .listener(requestListener)
                .into(new DrawableImageViewTarget(iconSecondary)).getRequest();
    }


    /**
     * This method is used to set TextIcon and make Icon hidden as there is no icon available for the token.
     *
     * @param token Token
     */
    private void setupTextIcon(@NotNull Token token)
    {
        icon.setImageResource(R.drawable.ic_clock);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(getColorStateList(getContext(), EthereumNetworkBase.getChainColour(token.tokenInfo.chainId)));
        //try symbol first
        if (!TextUtils.isEmpty(token.tokenInfo.symbol) && token.tokenInfo.symbol.length() > 1)
        {
            textIcon.setText(Utils.getIconisedText(token.tokenInfo.symbol));
        }
        else
        {
            textIcon.setText(Utils.getIconisedText(tokenName));
        }
    }

    public void setupFallbackTextIcon(String name)
    {
        textIcon.setText(name);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundTintList(getColorStateList(getContext(), EthereumNetworkBase.getChainColour(MAINNET_ID)));
    }

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback)
    {
        this.tokensAdapterCallback = tokensAdapterCallback;
    }

    private void performTokenClick(View v)
    {
        if (tokensAdapterCallback != null)
        {
            tokensAdapterCallback.onTokenClick(v, token, null, true);
        }
    }

    public void showLocalCurrency()
    {
        String isoCode = TickerService.getCurrencySymbolTxt();
        loadImageFromResource(CurrencyRepository.getFlagByISO(isoCode));
        token = null;
    }

    private void loadImageFromResource(int resourceId)
    {
        handler.removeCallbacks(null);
        statusBackground.setVisibility(View.GONE);
        chainIconBackground.setVisibility(View.GONE);
        chainIcon.setVisibility(View.GONE);
        textIcon.setVisibility(View.GONE);
        icon.setImageResource(resourceId);
        icon.setVisibility(View.VISIBLE);
        circle.setVisibility(View.VISIBLE);
    }

    public void setGrayscale(boolean grayscale)
    {
        if (grayscale)
        {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
            icon.setColorFilter(cf);
            icon.setImageAlpha(128);
        }
        else
        {
            icon.setColorFilter(null);
            icon.setImageAlpha(255);
        }
    }

    private void setIsAttestation(String symbol, long chainId)
    {
        loadImageFromResource(R.drawable.zero_one);
        textIcon.setVisibility(View.VISIBLE);
        textIcon.setBackgroundResource(0);
        textIcon.setText(Utils.getIconisedText(symbol));
        setChainIcon(chainId);
    }

    public void setSmartPassIcon(long chainId)
    {
        currentRq = Glide.with(this)
                .load(R.drawable.smart_pass)
                .apply(new RequestOptions().circleCrop())
                .listener(requestListener)
                .into(new DrawableImageViewTarget(icon)).getRequest();
        setChainIcon(chainId);
    }

    public void setAttestationIcon(String image, String symbol, long chain)
    {
        if (!TextUtils.isEmpty(image))
        {
            currentRq = Glide.with(this)
                    .load(image)
                    .placeholder(R.drawable.zero_one)
                    .apply(new RequestOptions().circleCrop())
                    .listener(requestListener)
                    .into(new DrawableImageViewTarget(icon)).getRequest();
        }
        else
        {
            setIsAttestation(symbol, chain);
        }
    }

    private Activity getActivity()
    {
        Context context = getContext();
        while (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
