package com.langitwallet.app.ui.widget.entity;

import static com.langitwallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.langitwallet.app.ui.widget.holder.TransactionHolder.TRANSACTION_BALANCE_PRECISION;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.langitwallet.app.R;
import com.langitwallet.app.entity.ActivityMeta;
import com.langitwallet.app.entity.Transaction;
import com.langitwallet.app.entity.tokens.Token;
import com.langitwallet.app.repository.EventResult;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public int getTitle()
    {
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

    public boolean isMintEvent()
    {
        return eventName.equals("received") && getDetailAddress().equals(ZERO_ADDRESS);
    }

    public String getOperationPrefix()
    {
        switch (eventName)
        {
            case "sent":
                return "-";
            case "received":
                return "+";
            default:
                return "";
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
                    if (tx != null && eResult.value.equals(ZERO_ADDRESS) && t != null)
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

    public String getToAddress()
    {
        Map<String, EventResult> resultMap = getEventResultMap();
        EventResult evTo = resultMap.get("to");
        if (evTo != null)
        {
            return evTo.value;
        }
        else
        {
            return null;
        }
    }

    public String getFromAddress()
    {
        Map<String, EventResult> resultMap = getEventResultMap();
        EventResult evFrom = resultMap.get("from");
        if (evFrom != null)
        {
            return evFrom.value;
        }
        else
        {
            return null;
        }
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

    public String getEventAmount(Token token, Transaction tx)
    {
        if (token == null)
        {
            return "";
        }

        if (tx != null)
        {
            tx.getDestination(token); //build decoded input
        }

        Map<String, EventResult> resultMap = getEventResultMap();
        String value = "";
        switch (eventName)
        {
            case "received":
                value = "+ ";
                //drop through
            case "sent":
                if (value.length() == 0) value = "- ";
                if (resultMap.get("amount") != null)
                {
                    value = token.convertValue(value, resultMap.get("amount"), TRANSACTION_BALANCE_PRECISION);
                }
                break;
            case "approvalObtained":
            case "ownerApproved":
                if (resultMap.get("value") != null)
                {
                    value = token.convertValue(value, resultMap.get("value"), TRANSACTION_BALANCE_PRECISION);
                }
                break;
            default:
                if (token != null && tx != null)
                {
                    value = token.isEthereum() ? token.getTransactionValue(tx, TRANSACTION_BALANCE_PRECISION) : tx.getOperationResult(token, TRANSACTION_BALANCE_PRECISION);
                }
                break;
        }

        return value;
    }
}
