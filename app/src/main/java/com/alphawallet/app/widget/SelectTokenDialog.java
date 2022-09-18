package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.Token;
import com.alphawallet.app.ui.widget.adapter.SelectTokenAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class SelectTokenDialog extends BottomSheetDialog
{
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView tokenList;
    private SelectTokenAdapter adapter;
    private LinearLayout searchLayout;
    private EditText search;
    private TextView noResultsText;

    public SelectTokenDialog(@NonNull Activity activity)
    {
        super(activity);
        View view = View.inflate(getContext(), R.layout.dialog_select_token, null);
        setContentView(view);

        setOnShowListener(dialogInterface -> {
            view.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
            behavior.setState(STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });

        tokenList = view.findViewById(R.id.token_list);
        search = view.findViewById(R.id.edit_search);
        searchLayout = view.findViewById(R.id.layout_search_tokens);
        noResultsText = view.findViewById(R.id.no_results);
        ImageView btnClose = view.findViewById(R.id.image_close);
        btnClose.setOnClickListener(v -> dismiss());
    }

    public SelectTokenDialog(List<Token> tokenItems, Activity activity, SelectTokenDialogEventListener callback)
    {
        this(activity);

        noResultsText.setVisibility(tokenItems.size() > 0 ? View.GONE : View.VISIBLE);

        adapter = new SelectTokenAdapter(tokenItems, callback);

        tokenList.setLayoutManager(new LinearLayoutManager(getContext()));
        tokenList.setAdapter(adapter);

        searchLayout.setOnClickListener(v -> {
        });

        search.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void afterTextChanged(final Editable searchFilter)
            {
                handler.postDelayed(() -> {
                    if (adapter != null)
                    {
                        adapter.filter(searchFilter.toString());
                    }
                }, 200);
            }
        });
    }

    public void setSelectedToken(String address)
    {
        adapter.setSelectedToken(address);
    }

    public interface SelectTokenDialogEventListener
    {
        void onChainSelected(Token tokenItem);
    }
}
