package com.alphawallet.app.entity.walletconnect;

import com.alphawallet.app.walletconnect.entity.WCEthereumSignMessage;
import com.alphawallet.app.walletconnect.entity.WCEthereumTransaction;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JB on 10/09/2020.
 */
public class WCRequest implements Parcelable
{
    public final long id;
    public final String sessionId;
    public final WCEthereumTransaction tx;
    public final WCEthereumSignMessage sign;
    public final WCPeerMeta peer;
    public final SignType type;
    public final long chainId;
    public final Throwable throwable;

    public WCRequest(String sessionId, long id, WCEthereumSignMessage msg)
    {
        this.sessionId = sessionId;
        this.id = id;
        this.sign = msg;
        type = SignType.MESSAGE;
        tx = null;
        peer = null;
        chainId = MAINNET_ID;
        throwable = null;
    }

    public WCRequest(String sessionId, long id, WCEthereumTransaction tx, boolean signOnly, long chainId)
    {
        this.sessionId = sessionId;
        this.id = id;
        this.sign = null;
        type = signOnly ? SignType.SIGN_TX : SignType.SEND_TX;
        this.tx = tx;
        this.chainId = chainId;
        peer = null;
        throwable = null;
    }

    public WCRequest(String sessionId, long id, WCPeerMeta pm, long chainId)
    {
        this.sessionId = sessionId;
        this.id = id;
        this.sign = null;
        this.chainId = chainId;
        type = SignType.SESSION_REQUEST;
        tx = null;
        peer = pm;
        throwable = null;
    }

    public WCRequest(String sessionId, Throwable t, long chainId)
    {
        this.sessionId = sessionId;
        this.id = 0;
        this.sign = null;
        this.chainId = chainId;
        type = SignType.FAILURE;
        tx = null;
        peer = null;
        throwable = t;
    }

    public static final Creator<WCRequest> CREATOR = new Creator<WCRequest>() {
        @Override
        public WCRequest createFromParcel(Parcel in) {
            return new WCRequest(in);
        }

        @Override
        public WCRequest[] newArray(int size) {
            return new WCRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(id);
        dest.writeString(sessionId);
        dest.writeInt(type.ordinal());
        dest.writeLong(chainId);

        // WCEthTxn
        dest.writeByte( (byte) (tx != null ? 1 : 0) );
        if (tx != null) {
            dest.writeString(tx.getFrom());
            writeStringToParcelIfNotNull(dest, tx.getTo());
            writeStringToParcelIfNotNull(dest, tx.getNonce());
            writeStringToParcelIfNotNull(dest, tx.getGasPrice());
            writeStringToParcelIfNotNull(dest, tx.getMaxFeePerGas());
            writeStringToParcelIfNotNull(dest, tx.getMaxPriorityFeePerGas());
            writeStringToParcelIfNotNull(dest, tx.getGas());
            writeStringToParcelIfNotNull(dest, tx.getGasLimit());
            writeStringToParcelIfNotNull(dest, tx.getValue());
            writeStringToParcelIfNotNull(dest, tx.getData());
        }

        // WCEthSignMsg
        dest.writeByte( (byte) (sign != null ? 1 : 0) );
        if (sign != null) {
            dest.writeList(sign.getRaw());
            dest.writeInt(sign.getType().ordinal());
        }

        // WCPeerMeta
        dest.writeByte((byte) (peer != null ? 1 : 0));
        if (peer != null) {
            dest.writeString(peer.getName());
            dest.writeString(peer.getUrl());
            writeStringToParcelIfNotNull(dest, peer.getDescription());
            dest.writeList(peer.getIcons());
        }

        // throwable TODO write stacktrace into parcel if possible
        dest.writeByte((byte) (throwable != null ? 1 : 0));
        if (throwable != null) {
            dest.writeString(throwable.getMessage());       //only writing message for throwable
        }

    }

    protected WCRequest(Parcel in) {
        id = in.readLong();
        sessionId = in.readString();
        type = SignType.values()[in.readInt()];
        chainId = in.readLong();

        // WCEthTxn
        if (in.readByte() == 1) {
            String from = in.readString();
            String to = readStringFromParcelIfNotNull(in);
            String nonce = readStringFromParcelIfNotNull(in);
            String gasPrice = readStringFromParcelIfNotNull(in);
            String maxFeePerGas = readStringFromParcelIfNotNull(in);
            String maxPriorityFeePerGas = readStringFromParcelIfNotNull(in);
            String gas = readStringFromParcelIfNotNull(in);
            String gasLimit = readStringFromParcelIfNotNull(in);
            String value = readStringFromParcelIfNotNull(in);
            String checkData = readStringFromParcelIfNotNull(in);
            String data = checkData != null ? checkData : "";

            this.tx = new WCEthereumTransaction(
                    from,
                    to,
                    nonce,
                    gasPrice,
                    maxFeePerGas,
                    maxPriorityFeePerGas,
                    gas,
                    gasLimit,
                    value,
                    data
            );
        } else {
            this.tx = null;
        }

        // WCEthSignMsg
        if (in.readByte() == 1) {
            List<String> raw = new ArrayList<>();
            in.readList(raw, List.class.getClassLoader());
            WCEthereumSignMessage.WCSignType type = WCEthereumSignMessage.WCSignType.values()[in.readInt()];
            this.sign = new WCEthereumSignMessage(raw, type);
        } else {
            this.sign = null;
        }

        //WCPeerMeta
        if (in.readByte() == 1) {
            String name = in.readString();
            String url = in.readString();
            String description = readStringFromParcelIfNotNull(in);
            List<String> icons = new ArrayList<>();
            in.readList(icons, List.class.getClassLoader());
            this.peer = new WCPeerMeta(name, url, description, icons);
        } else {
            this.peer = null;
        }

        if (in.readByte() == 1) {
            throwable = new Throwable(in.readString());
        } else {
            throwable = null;
        }
    }

    /** Writes a byte indicating whether the value is present. If present, writes the value as well */
    private void writeStringToParcelIfNotNull(Parcel parcel, String valueToWrite) {
        parcel.writeByte((byte) (valueToWrite != null ? 1 : 0));
        if (valueToWrite != null) {
            parcel.writeString(valueToWrite);
        }
    }

    /** Reads the value if the first byte is 1. */
    private String readStringFromParcelIfNotNull(Parcel parcel) {
        boolean isPresent = ((byte) parcel.readByte()) == 1;
        if (isPresent) {
            return parcel.readString();
        }
        return null;
    }
 }
