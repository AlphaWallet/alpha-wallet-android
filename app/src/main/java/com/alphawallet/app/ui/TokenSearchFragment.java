package com.alphawallet.app.ui;

import static com.alphawallet.app.ui.WalletFragment.SEARCH_FRAGMENT;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.SearchToolbarCallback;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.widget.SearchToolbar;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


/**
 * Created by JB on 11/12/2021.
 */
@AndroidEntryPoint
public class TokenSearchFragment extends Fragment implements SearchToolbarCallback, TokensAdapterCallback
{
    private WalletViewModel viewModel;
    private TokensAdapter adapter;
    private RecyclerView recyclerView;
    private View selectedToken;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {

        final View view = inflater.inflate(R.layout.fragment_wallet_search, container, false);

        SearchToolbar searchBar = view.findViewById(R.id.search);
        recyclerView = view.findViewById(R.id.list);
        searchBar.setSearchCallback(this);

        searchBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            searchBar.getEditView().requestFocus();
            KeyboardUtils.showKeyboard(searchBar.getEditView());
        });

        initViewModel();
        initList();
        setupKeyboardViewResizer(view, searchBar);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                backPressed();
            }
        });

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
        {
            backPressed();
            return true;
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }
    }

    private void initList() {
        adapter = new TokensAdapter(this, viewModel.getAssetDefinitionService(), viewModel.getTokensService(),
                null);
        adapter.setHasStableIds(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        if (recyclerView.getItemAnimator() != null)
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        recyclerView.addRecyclerListener(holder -> adapter.onRViewRecycled(holder));
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void searchText(String search)
    {
        if (viewModel != null)
        {
            viewModel.searchTokens(search);
            adapter.clear();
        }
    }

    @Override
    public void backPressed()
    {
        //terminate fragment
        Bundle result = new Bundle();
        result.putBoolean(SEARCH_FRAGMENT, true);
        getParentFragmentManager().setFragmentResult(SEARCH_FRAGMENT, result);
        KeyboardUtils.hideKeyboard(getView());
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(WalletViewModel.class);
        viewModel.tokens().observe(getViewLifecycleOwner(), this::onTokens);
        viewModel.prepare();
    }

    private void onTokens(TokenCardMeta[] tokens)
    {
        adapter.setTokens(tokens);
        viewModel.calculateFiatValues();
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected)
    {
        //launch token
        if (selectedToken != null) return;
        getParentFragmentManager().setFragmentResult(C.TOKEN_CLICK, new Bundle());
        selectedToken = view;
        Token clickOrigin = viewModel.getTokenFromService(token);
        if (clickOrigin == null) clickOrigin = token;
        viewModel.showTokenDetail(getActivity(), clickOrigin);
        //terminate this fragment
        handler.postDelayed(() -> {
            selectedToken = null;
            backPressed();
        },700);
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds)
    {

    }

    private void setupKeyboardViewResizer(final View view, final SearchToolbar topBar)
    {
        KeyboardVisibilityEvent.setEventListener(
                getActivity(), isOpen -> {
            if (isOpen)
            {
                Rect r = new Rect();
                topBar.getWindowVisibleDisplayFrame(r); //allow for fullscreen or notification bar
                int topBarStart = r.top;
                view.getWindowVisibleDisplayFrame(r);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
                layoutParams.bottomMargin = view.getRootView().getHeight() - (r.bottom - r.top) - topBarStart;
                recyclerView.setLayoutParams(layoutParams);
            }
            else
            {
                if (getActivity() == null) return;
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
                layoutParams.bottomMargin = ((HomeActivity) getActivity()).getNavBarHeight();
                recyclerView.setLayoutParams(layoutParams);
            }
        });
    }
}
