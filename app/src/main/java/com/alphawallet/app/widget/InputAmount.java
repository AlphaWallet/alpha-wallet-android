package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.Utils;

import io.realm.Case;
import io.realm.Realm;

import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;

/**
 * Created by JB on 10/11/2020.
 */
public class InputAmount extends LinearLayout
{
    private final Context context;
    private final EditText editText;
    private final TextView symbolText;
    private final TokenIcon icon;
    private final TextView chainName;
    private final TextView availableSymbol;
    private final TextView availableAmount;
    private Token token;
    private Realm realm;
    private TokensService tokensService;

    public InputAmount(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        inflate(context, R.layout.item_input_amount_2, this);

        editText = findViewById(R.id.amount_entry);
        symbolText = findViewById(R.id.text_token_symbol);
        icon = findViewById(R.id.token_icon);
        chainName = findViewById(R.id.text_chain_name);
        availableSymbol = findViewById(R.id.text_symbol);
        availableAmount = findViewById(R.id.text_available);

        setupViewListeners();
    }

    public void setupToken(Token token, AssetDefinitionService assetDefinitionService, TokensService svs, Realm realm)
    {
        this.token = token;
        this.tokensService = svs;
        this.realm = realm;
        icon.bindData(token, assetDefinitionService);
        symbolText.setText(token.getSymbol());
        Utils.setChainColour(chainName, token.tokenInfo.chainId);
        chainName.setText(token.getNetworkName());
        availableSymbol.setText(token.getSymbol());
        availableAmount.setText(token.getStringBalance());

        bindDataSource();
    }

    /**
     * Setup realm binding
     */
    private void bindDataSource()
    {
        String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
        RealmToken realmUpdate = realm.where(RealmToken.class)
                .equalTo("address", dbKey, Case.INSENSITIVE)
                .findFirstAsync();

        realmUpdate.addChangeListener(realmToken -> {
            RealmToken rt = (RealmToken) realmToken;
            //load token & update balance
            token = tokensService.getToken(rt.getChainId(), rt.getTokenAddress());
            availableAmount.setText(token.getStringBalance());
        });
    }

    public void onDestroy()
    {
        if (realm != null) realm.removeAllChangeListeners();
    }

    private void setupViewListeners()
    {
        TextView allFunds = findViewById(R.id.text_all_funds);
        LinearLayout clickMore = findViewById(R.id.layout_more_click);

        allFunds.setOnClickListener(v -> {
            //on all funds
        });

        clickMore.setOnClickListener(v -> {
            //on down caret clicked - switch to fiat currency equivalent if there's a ticker
            //load ticker
            RealmTokenTicker rtt = realm.where(RealmTokenTicker.class)
                    .equalTo("contract", TokensRealmSource.databaseKey(token.tokenInfo.chainId, token.isEthereum() ? "eth" : token.getAddress().toLowerCase()))
                    .findFirst();

            if (rtt != null)
            {
                //show the
            }
        });
    }

}
