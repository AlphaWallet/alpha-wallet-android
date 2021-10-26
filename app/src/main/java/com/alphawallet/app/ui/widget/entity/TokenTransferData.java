package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EventResult;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

/**
 * Created by JB on 17/12/2020.
 */
public class TokenTransferData extends ActivityMeta implements Parcelable
{
    public final long chainId;
    public final String tokenAddress;
    public final String eventName;
    public final String transferDetail;

    public TokenTransferData(String hash, long chainId, String tokenAddress, String eventName, String transferDetail, long transferTime)
    {
        super(transferTime, hash, true);
        this.chainId = chainId;
        this.tokenAddress = tokenAddress;
        this.eventName = eventName;
        this.transferDetail = transferDetail;
    }

    public int getTitle(Transaction tx)
    {
        //TODO: pick up item-view
        //catch standard Token events
        switch (eventName)
        {
            case "sent":
                String to = getDetailAddress();
                if (!TextUtils.isEmpty(to) && to.equals(ZERO_ADDRESS))
                {
                    return R.string.token_burn;
                }
                else
                {
                    return R.string.activity_sent;
                }
            case "received":
                String from = getDetailAddress();
                if (!TextUtils.isEmpty(from) && from.equals(ZERO_ADDRESS))
                {
                    return R.string.token_mint;
                }
                else
                {
                    return R.string.activity_received;
                }
            case "ownerApproved":
                return R.string.activity_approved;
            case "approvalObtained":
                return R.string.activity_approval_granted;
            default:
                return 0;
        }
    }

    protected TokenTransferData(Parcel in)
    {
        super(in.readLong(), in.readString(), true);
        chainId = in.readLong();
        tokenAddress = in.readString();
        eventName = in.readString();
        transferDetail = in.readString();
    }

    public static final Creator<TokenTransferData> CREATOR = new Creator<TokenTransferData>() {
        @Override
        public TokenTransferData createFromParcel(Parcel in) {
            return new TokenTransferData(in);
        }

        @Override
        public TokenTransferData[] newArray(int size) {
            return new TokenTransferData[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(timeStamp);
        dest.writeString(hash);
        dest.writeLong(chainId);
        dest.writeString(tokenAddress);
        dest.writeString(eventName);
        dest.writeString(transferDetail);
    }

    private enum ResultState
    {
        NAME,
        TYPE,
        RESULT
    }

    public String getDetail(Context ctx, Transaction tx, Token t, final String itemView)
    {
        return getDetail(ctx, tx, itemView, t, true);
    }

    public String getDetail(Context ctx, Transaction tx, final String itemView, Token t, boolean shrinkAddress)
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
                    eventDesc = eventDesc.substring(0, index) + replacement + eventDesc.substring(index + m.group(0).length());
                }
            }

            return eventDesc;
        }
        //catch standard Token events
        switch (eventName)
        {
            case "sent":
                if (resultMap.containsKey("to"))
                    return shrinkAddress ? ctx.getString(R.string.sent_to, ENSHandler.displayAddressOrENS(ctx, resultMap.get("to").value))
                            : ENSHandler.displayAddressOrENS(ctx, resultMap.get("to").value, false);
                break;
            case "received":
                EventResult eResult = resultMap.get("from");
                if (eResult != null)
                {
                    if (tx != null && eResult.value.equals(ZERO_ADDRESS))
                    {
                        return t.getFullName();
                    }
                    else
                    {
                        return shrinkAddress ? ctx.getString(R.string.from, ENSHandler.displayAddressOrENS(ctx, eResult.value))
                                : ENSHandler.displayAddressOrENS(ctx, eResult.value, false);
                    }
                }
                break;
            case "ownerApproved":
                if (resultMap.containsKey("spender"))
                    return ctx.getString(R.string.approval_granted_to, ENSHandler.displayAddressOrENS(ctx, resultMap.get("spender").value, shrinkAddress));
                break;
            case "approvalObtained":
                if (resultMap.containsKey("owner"))
                    return ctx.getString(R.string.approval_obtained_from, ENSHandler.displayAddressOrENS(ctx, resultMap.get("owner").value, shrinkAddress));
                break;
            default:
                //use name of event
                return eventName;
        }

        return eventName;
    }

    public Map<String, EventResult> getEventResultMap()
    {
        String[] split = transferDetail.split(",");
        ResultState state = ResultState.NAME;
        Map<String, EventResult> resultMap = new HashMap<>();
        String name = null;
        String type = null;
        for (String r : split)
        {
            switch (state)
            {
                case NAME:
                    name = r;
                    state = ResultState.TYPE;
                    break;
                case TYPE:
                    type = r;
                    state = ResultState.RESULT;
                    break;
                case RESULT:
                    if (name != null && type != null)
                    {
                        resultMap.put(name, new EventResult(type, r));
                        name = null;
                        type = null;
                    }
                    state = ResultState.NAME;
                    break;
            }
        }

        return resultMap;
    }

    public String getDetailAddress()
    {
        Map<String, EventResult> resultMap = getEventResultMap();
        switch (eventName)
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

        return eventName;
    }

    public StatusType getEventStatusType()
    {
        switch (eventName)
        {
            case "sent":
                return StatusType.SENT;
            case "received":
                return StatusType.RECEIVE;
            case "ownerApproved":
            case "approvalObtained":
            default:
                //use name of event
                return StatusType.NONE;
        }
    }

}
