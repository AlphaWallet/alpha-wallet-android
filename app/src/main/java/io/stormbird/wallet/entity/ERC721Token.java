package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Token extends Token implements Parcelable
{
    List<OpenseaElement> tokenBalance = new ArrayList<>();

    public ERC721Token(TokenInfo tokenInfo, List<OpenseaElement> balanceList, long blancaTime) {
        super(tokenInfo, BigDecimal.ZERO, blancaTime);
        tokenBalance = balanceList;
    }

    private ERC721Token(Parcel in) {
        super(in);
        //read in the element list
        int size = in.readInt();
        for (; size > 0; size--)
        {
            OpenseaElement element = in.readParcelable(OpenseaElement.class.getClassLoader());
            tokenBalance.add(element);
        }
    }

    public static final Creator<ERC721Token> CREATOR = new Creator<ERC721Token>() {
        @Override
        public ERC721Token createFromParcel(Parcel in) {
            return new ERC721Token(in);
        }

        @Override
        public ERC721Token[] newArray(int size) {
            return new ERC721Token[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(tokenBalance.size());
        for (OpenseaElement element : tokenBalance)
        {
            dest.writeParcelable(element, flags);
        }
    }


}
