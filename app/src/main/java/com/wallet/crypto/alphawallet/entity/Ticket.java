package com.wallet.crypto.alphawallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.repository.entity.RealmToken;
import com.wallet.crypto.alphawallet.ui.AddTokenActivity;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.ethereum.geth.BigInt;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 27/01/2018.
 */

public class Ticket extends Token implements Parcelable
{
    public final List<Integer> balanceArray;
    private List<Integer> burnArray;

    public Ticket(TokenInfo tokenInfo, List<Integer> balances, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = balances;
        burnArray = new ArrayList<>();
    }

    public Ticket(TokenInfo tokenInfo, String balances, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        this.balanceArray = parseIDListInteger(balances);
        burnArray = new ArrayList<>();
    }

    private Ticket(Parcel in) {
        super(in);
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        Object[] readBurnArray = in.readArray(Object.class.getClassLoader());
        balanceArray = new ArrayList<Integer>();
        burnArray = new ArrayList<Integer>();
        for (Object o : readObjArray)
        {
            Integer val = (Integer)o;
            balanceArray.add(val);
        }

        //check to see if burn notice is needed
        for (Object o : readBurnArray)
        {
            Integer val = (Integer)o;
            if (balanceArray.contains(val))
            {
                burnArray.add(val);
            }
        }
    }

    @Override
    public String getStringBalance() {
        return populateIDs(balanceArray, false);// parseList(balanceArray);
    }

    @Override
    public String getFullBalance() {
        if (balanceArray == null) return "no tokens";
        else return populateIDs(balanceArray, true);
    }

    public static final Creator<Ticket> CREATOR = new Creator<Ticket>() {
        @Override
        public Ticket createFromParcel(Parcel in) {
            return new Ticket(in);
        }

        @Override
        public Ticket[] newArray(int size) {
            return new Ticket[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeArray(balanceArray.toArray());
        dest.writeArray(burnArray.toArray());
    }

    public String parseList(List<Integer> checkedIndexList) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        if (checkedIndexList != null)
        {
            for (Integer i : checkedIndexList)
            {
                //reverse lookup the selected IDs
                if (i < balanceArray.size())
                {
                    if (!first) sb.append(", ");
                    int thisTicketId = balanceArray.get(i);
                    sb.append(String.valueOf(thisTicketId));
                    first = false;
                }
            }
        }
        else {
            sb.append("none");
        }

        return sb.toString();
    }

    public String checkBalance(String selection)
    {
        StringBuilder sb = new StringBuilder();
        //convert selection to index list
        List<org.web3j.abi.datatypes.generated.Int16> selectionIndex = parseIDList(selection);
        //add correct entries
        boolean first = true;
        for (org.web3j.abi.datatypes.generated.Int16 id : selectionIndex) {
            if (balanceArray.contains(id.getValue().intValue()) && !burnArray.contains(id.getValue().intValue())) {
                if (!first) sb.append(", ");
                sb.append(String.valueOf(id.getValue().toString(10)));
                first = false;
            }
        }

        return sb.toString();
    }

    public List<org.web3j.abi.datatypes.generated.Int16> parseIDList(String userList)
    {
        List<org.web3j.abi.datatypes.generated.Int16> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                org.web3j.abi.datatypes.generated.Int16 thisId = new org.web3j.abi.datatypes.generated.Int16(Integer.parseInt(trim));
                idList.add(thisId);
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }

    public List<BigInteger> parseIDListBI(String userList)
    {
        List<BigInteger> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                BigInteger thisId = BigInteger.valueOf(Integer.parseInt(trim));
                idList.add(thisId);
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }

    public List<Integer> parseIDListInteger(String userList)
    {
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                idList.add(Integer.parseInt(trim));
            }
        }
        catch (Exception e)
        {
            idList = null;
        }

        return idList;
    }

    @Override
    public List<Integer> parseIndexList(String userList)
    {
        //read given indicies and convert into internal format, error checking to ensure
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids) {
                //remove whitespace
                String trim = id.trim();
                Integer thisId = Integer.parseInt(trim);

                if (thisId > 0) {
                    int index = balanceArray.indexOf(thisId);
                    if (index > -1) {
                        if (!idList.contains(index)) {  //just make sure they didn't already add this one
                            idList.add(index);
                        }
                    } else {
                        idList = null;
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            idList = null;
        }

        return idList;
    }

    //Burn handling
    public void addToBurnList(List<BigInteger> burnIndicies)
    {
        for (BigInteger b : burnIndicies) {
            Integer index = b.intValue();

            //lookup index
            if (balanceArray.size() > index)
            {
                Integer value = balanceArray.get(index);
                if (value > 0) {
                    burnArray.add(value);
                }
            }
        }
    }

    public List<Integer> getValidIndicies() {
        List<Integer> validIndicies = new ArrayList<>();
        for (Integer ticketIndex : balanceArray)
        {
            if (!burnArray.contains(ticketIndex)) {
                validIndicies.add(ticketIndex);
            }
        }

        return validIndicies;
    }

    @Override
    public String populateIDs(List<Integer> idArray, boolean keepZeros)
    {
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : idArray)
        {
            if (!keepZeros && id == 0) continue;
            if (!first)
            {
                sb.append(", ");
            }
            first = false;

            sb.append(id.toString());
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    @Override
    public int getTicketCount()
    {
        int count = 0;
        for (Integer id : balanceArray)
        {
            if (id > 0) count++;
        }
        return count;
    }

    @Override
    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(populateIDs(balanceArray, true));
    }

    @Override
    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showUseToken(context, this);
    }

    @Override
    public void setupContent(TokenHolder tokenHolder)
    {
        tokenHolder.fillIcon(null, R.mipmap.ic_alpha);
        tokenHolder.balanceEth.setVisibility(View.GONE);
        tokenHolder.balanceCurrency.setVisibility(View.GONE);
        tokenHolder.arrayBalance.setVisibility(View.VISIBLE);

        //String ids = populateIDs(((Ticket)(tokenHolder.token)).balanceArray, false);
        tokenHolder.arrayBalance.setText(String.valueOf(getTicketCount()) + " Tickets");
    }

    public String populateRange(TicketRange range)
    {
        StringBuilder sb = new StringBuilder();
        //find all ticket indexes in this range
        //find start ID
        int index = getIndexOf(range.tokenId); //TODO: replace with Numeric map

        boolean first = true;

        if (index > -1)
        {
            for (int i = 0; i < range.seatCount; i++)
            {
                int id = balanceArray.get(index + i);
                if (id > 0)
                {
                    if (!first) sb.append(", ");
                    sb.append(String.valueOf(id));
                    first = false;
                }
            }
        }
        else
        {
            sb.append("none");
        }

        return sb.toString();
    }

    private int getIndexOf(int id)
    {
        if (balanceArray.contains(id))
        {
            return balanceArray.indexOf(id);
        }
        else
        {
            return -1;
        }
    }
}
