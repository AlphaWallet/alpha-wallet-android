package com.alphawallet.app.repository.entity;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EventResult;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.Utils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;

import static com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS;

/**
 * Created by James on 6/05/2019.
 * Stormbird in Sydney
 */
public class RealmAuxData extends RealmObject
{
    @PrimaryKey
    private String instanceKey; //should be token address, token Id, chainId
    private long chainId;
    private String tokenAddress;
    private String tokenId;
    private String functionId;
    private String result;
    private long resultTime;
    private long resultReceivedTime; //allows us to filter new events

    public String getInstanceKey()
    {
        return instanceKey;
    }

    public long getChainId()
    {
        return chainId;
    }

    public String getTransactionHash()
    {
        String[] split = instanceKey.split("-");
        if (split.length > 0) return split[0];
        else return "";
    }

    public String getEventName()
    {
        String[] split = instanceKey.split("-");
        if (split.length > 1) return split[1];
        else return "";
    }

    public long getExtendId()
    {
        String[] split = instanceKey.split("-");
        if (split.length > 3)
        {
            String extendId = split[3];
            if (extendId != null && extendId.length() > 0 && Character.isDigit(extendId.charAt(0)))
            {
                return Long.parseLong(extendId);
            }
        }

        return 0;
    }

    public void setChainId(long chainId)
    {
        this.chainId = chainId;
    }

    public BigInteger getTokenId()
    {
        try
        {
            return new BigInteger(tokenId, Character.MAX_RADIX);
        }
        catch (Exception e)
        {
            return BigInteger.ZERO;
        }
    }

    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }

    public String getFunctionId()
    {
        return functionId;
    }

    public void setFunctionId(String functionId)
    {
        this.functionId = functionId;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public long getResultTime()
    {
        return resultTime;
    }

    public void setResultTime(long resultTime)
    {
        this.resultTime = resultTime;
    }

    public String getAddress()
    {
        return instanceKey.split("-")[0];
    }

    public String getTokenAddress()
    {
        return tokenAddress;
    }

    public void setTokenAddress(String address)
    {
        tokenAddress = address;
    }

    public void setResultReceivedTime(long resultReceivedTime)
    {
        this.resultReceivedTime = resultReceivedTime;
    }

    public long getResultReceivedTime()
    {
        return resultReceivedTime;
    }


    public StatusType getEventStatusType()
    {
        switch (getFunctionId())
        {
            case "sent":
                return StatusType.SENT;
            case "received":
                return StatusType.RECEIVE;
            case "mint":
                return StatusType.RECEIVE;
            case "burn":
                return StatusType.SENT;
            case "ownerApproved":
            case "approvalObtained":
            default:
                //use name of event
                return StatusType.NONE;
        }
    }

    public String getDetailAddress()
    {
        Map<String, EventResult> resultMap = getEventResultMap();
        switch (getFunctionId())
        {
            case "sent":
                if (resultMap.containsKey("to")) return resultMap.get("to").value;
                break;
            case "received":
                if (resultMap.containsKey("from")) return resultMap.get("from").value;
                break;
            case "ownerApproved":
                if (resultMap.containsKey("spender")) return resultMap.get("spender").value;
                break;
            case "approvalObtained":
                if (resultMap.containsKey("owner")) return resultMap.get("owner").value;
                break;
            default:
                //use name of event
                break;
        }

        return getEventName();
    }

    public String getDetail(Context ctx, Transaction tx, final String itemView)
    {
        Map<String, EventResult> resultMap = getEventResultMap();
        if (tx != null) tx.addTransactionElements(resultMap);

        if (!TextUtils.isEmpty(itemView))
        {
            String eventDesc = itemView;
            Matcher m = Pattern.compile("\\$\\{([^}]+)\\}").matcher(itemView);
            while (m.find())
            {
                String match = m.group(1);
                String replacement = resultMap.containsKey(match) ? resultMap.get(match).value : null;
                if (replacement != null)
                {
                    int index = eventDesc.indexOf(m.group(0));
                    eventDesc = eventDesc.substring(0,index) + replacement + eventDesc.substring(index + m.group(0).length());
                }
            }

            return eventDesc;
        }
        //catch standard Token events
        switch (getFunctionId())
        {
            case "sent":
                if (resultMap.containsKey("to")) return ctx.getString(R.string.sent_to, ENSHandler.displayAddressOrENS(ctx, resultMap.get("to").value));
                break;
            case "received":
                if (resultMap.containsKey("from")) return ctx.getString(R.string.from, ENSHandler.displayAddressOrENS(ctx, resultMap.get("from").value));
                break;
            case "ownerApproved":
                if (resultMap.containsKey("spender")) return ctx.getString(R.string.approval_granted_to, ENSHandler.displayAddressOrENS(ctx, resultMap.get("spender").value));
                break;
            case "approvalObtained":
                if (resultMap.containsKey("owner")) return ctx.getString(R.string.approval_obtained_from, ENSHandler.displayAddressOrENS(ctx, resultMap.get("owner").value));
                break;
            default:
                //use name of event
                return getEventName();
        }

        //TODO: display event result
        return tokenId + " " + result;
    }

    public String getTitle(Context ctx)
    {
        //TODO: pick up item-view
        //catch standard Token events
        switch (getFunctionId())
        {
            case "sent":
                return ctx.getString(R.string.activity_sent);
            case "received":
                return ctx.getString(R.string.activity_received);
            case "ownerApproved":
                return ctx.getString(R.string.activity_approved);
            case "approvalObtained":
                return ctx.getString(R.string.activity_approval_granted);
            default:
                //already set up
                return getFunctionId();
                //display non indexed value
                //getString(R.string.valueSymbol, transactionValue, sym)
        }
    }

    private enum resultState {
        NAME,
        TYPE,
        RESULT
    }

    public Map<String, EventResult> getEventResultMap()
    {
        String[] split = result.split(",");
        resultState state = resultState.NAME;
        Map<String, EventResult> resultMap = new HashMap<>();
        String name = null;
        String type = null;
        for (String r : split)
        {
            switch (state)
            {
                case NAME:
                    name = r;
                    state = resultState.TYPE;
                    break;
                case TYPE:
                    type = r;
                    state = resultState.RESULT;
                    break;
                case RESULT:
                    if (name != null && type != null)
                    {
                        resultMap.put(name, new EventResult(type, r));
                        name = null;
                        type = null;
                    }
                    state = resultState.NAME;
                    break;
            }
        }

        return resultMap;
    }

    public static RealmResults<RealmAuxData> getEventListener(Realm realm, Token token, BigInteger tokenId, int historyCount, long timeLimit)
    {
        return getEventQuery(realm, token, tokenId, historyCount, timeLimit).findAllAsync();
    }

    public static RealmQuery<RealmAuxData> getEventQuery(Realm realm, Token token, BigInteger tokenId, int historyCount, long timeLimit)
    {
        String tokenIdHex = tokenId.toString(16);
        return realm.where(RealmAuxData.class)
                .endsWith("instanceKey", EVENT_CARDS)
                .sort("resultTime", Sort.DESCENDING)
                .greaterThan("resultTime", timeLimit)
                .equalTo("chainId", token.tokenInfo.chainId)
                .beginGroup().equalTo("tokenId", "0").or().equalTo("tokenId", tokenIdHex).endGroup()
                .equalTo("tokenAddress", token.getAddress())
                .limit(historyCount);
    }
}
