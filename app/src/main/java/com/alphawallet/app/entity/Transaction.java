package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EventResult;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;
import com.google.gson.annotations.SerializedName;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_ID;

/**
 *
 * This is supposed to be a generic transaction class which can
 * contain all of 3 stages of a transaction:
 * 
 * 1. being compiled, in progress, or ready to be signed;
 * 2. compiled and signed, or ready to be broadcasted;
 * 2. already broadcasted, obtained in its raw format from a node, 
 *    including the signatures in it;
 * 4. already included in a blockchain.
 */
public class Transaction implements Parcelable
{
    @SerializedName("id")
    public final String hash;
    public final String blockNumber;
    public final long timeStamp;
    public final int nonce;
    public final String from;
    public final String to;
    public final String value;
    public final String gas;
    public final String gasPrice;
    public final String gasUsed;
    public final String input;
    public final String error;
    public final long chainId;

    public boolean isConstructor = false;
    public TransactionInput transactionInput = null;

    public static final String CONSTRUCTOR = "Constructor";
	public static final TransactionDecoder decoder = new TransactionDecoder();
	public static ParseMagicLink parser = null;

	//placeholder for error
	public Transaction()
	{
		//blank transaction
		hash = "";
		blockNumber = "";
		timeStamp = 0;
		nonce = 0;
		from = "";
		to = "";
		value = "";
		gas = "";
		gasPrice = "";
		gasUsed = "";
		input = "";
		error = "";
		chainId = 0;
	}

	public boolean isPending()
	{
		return TextUtils.isEmpty(blockNumber) || blockNumber.equals("0") || blockNumber.equals("-2");
	}

	public boolean hasError()
	{
		return !TextUtils.isEmpty(error) && error.equals("1");
	}

	public boolean hasData()
	{
		return !TextUtils.isEmpty(input) && input.length() > 2;
	}

    public Transaction(
            String hash,
            String error,
            String blockNumber,
            long timeStamp,
			int nonce,
			String from,
			String to,
			String value,
			String gas,
			String gasPrice,
			String input,
			String gasUsed,
            long chainId,
            boolean isConstructor) {
        this.hash = hash;
        this.error = error;
        this.blockNumber = blockNumber;
        this.timeStamp = timeStamp;
		this.nonce = nonce;
		this.from = from;
		this.to = to;
		this.value = value;
		this.gas = gas;
		this.gasPrice = gasPrice;
		this.input = input;
		this.gasUsed = gasUsed;
		this.chainId = chainId;
		this.isConstructor = isConstructor;
	}

	public Transaction(Web3Transaction tx, long chainId, String wallet)
	{
		this.hash = null;
		this.error = null;
		this.blockNumber = null;
		this.timeStamp = System.currentTimeMillis()/1000;
		this.nonce = -1;
		this.from = wallet;
		this.to = tx.recipient.toString();
		this.value = tx.value.toString();
		this.gas = tx.gasLimit.toString();
		this.gasPrice = tx.gasPrice.toString();
		this.input = tx.payload;
		this.gasUsed = tx.gasLimit.toString();
		this.chainId = chainId;
		this.isConstructor = tx.isConstructor();
	}

	public Transaction(org.web3j.protocol.core.methods.response.Transaction ethTx, long chainId, boolean isSuccess, long timeStamp)
	{
		// Get contract address if constructor
		String contractAddress = ethTx.getCreates() != null ? ethTx.getCreates() : "";
		int nonce = ethTx.getNonceRaw() != null ? Numeric.toBigInt(ethTx.getNonceRaw()).intValue() : 0;

		if (!TextUtils.isEmpty(contractAddress)) //must be a constructor
		{
			to = contractAddress;
			isConstructor = true;
			input = CONSTRUCTOR;
		}
		else if (ethTx.getTo() == null && ethTx.getInput() != null && ethTx.getInput().startsWith("0x60"))
		{
			// some clients don't populate the 'creates' data for constructors. Note: Ethereum constructor always starts with a 'PUSH' 0x60 instruction
			input = CONSTRUCTOR;
			isConstructor = true;
			to = calculateContractAddress(ethTx.getFrom(), nonce);
		}
		else
		{
			this.to = ethTx.getTo() != null ? ethTx.getTo() : "";
			this.input = ethTx.getInput();
		}

		this.hash = ethTx.getHash();
		this.blockNumber = ethTx.getBlockNumber().toString();
		this.timeStamp = timeStamp;
		this.error = isSuccess ? "0" : "1";
		this.nonce = nonce;
		this.from = ethTx.getFrom();
		this.value = ethTx.getValue().toString();
		this.gas = ethTx.getGas().toString();
		this.gasPrice = ethTx.getGasPrice().toString();
		this.gasUsed = ethTx.getGas().toString();
		this.chainId = chainId;
	}

	public Transaction(String hash, String isError, String blockNumber, long timeStamp, int nonce, String from, String to,
					   String value, String gas, String gasPrice, String input, String gasUsed, long chainId, String contractAddress)
	{
		if (!TextUtils.isEmpty(contractAddress)) //must be a constructor
		{
			to = contractAddress;
			isConstructor = true;
			input = CONSTRUCTOR;
		}

		this.to = to;
		this.hash = hash;
		this.error = isError;
		this.blockNumber = blockNumber;
		this.timeStamp = timeStamp;
		this.nonce = nonce;
		this.from = from;
		this.value = value;
		this.gas = gas;
		this.gasPrice = gasPrice;
		this.input = input;
		this.gasUsed = gasUsed;
		this.chainId = chainId;
	}

	protected Transaction(Parcel in)
	{
		hash = in.readString();
		error = in.readString();
		blockNumber = in.readString();
		timeStamp = in.readLong();
		nonce = in.readInt();
		from = in.readString();
		to = in.readString();
		value = in.readString();
		gas = in.readString();
		gasPrice = in.readString();
		input = in.readString();
		gasUsed = in.readString();
		chainId = in.readLong();
	}

	public static final Creator<Transaction> CREATOR = new Creator<Transaction>() {
		@Override
		public Transaction createFromParcel(Parcel in) {
			return new Transaction(in);
		}

		@Override
		public Transaction[] newArray(int size) {
			return new Transaction[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hash);
        dest.writeString(error);
        dest.writeString(blockNumber);
        dest.writeLong(timeStamp);
		dest.writeInt(nonce);
		dest.writeString(from);
		dest.writeString(to);
		dest.writeString(value);
		dest.writeString(gas);
		dest.writeString(gasPrice);
		dest.writeString(input);
		dest.writeString(gasUsed);
		dest.writeLong(chainId);
	}

	public boolean isRelated(String contractAddress, String walletAddress)
	{
		if (contractAddress.equals("eth"))
		{
			return (input.equals("0x") || from.equalsIgnoreCase(walletAddress));
		}
		else if (walletAddress.equalsIgnoreCase(contractAddress)) //transactions sent from or sent to the main currency account
		{
			return from.equalsIgnoreCase(walletAddress) || to.equalsIgnoreCase(walletAddress);
		}
		else if (to.equalsIgnoreCase(contractAddress))
		{
			return true;
		}
		else
		{
			return getWalletInvolvedInTransaction(walletAddress);
		}
	}

	/**
	 * Fetch result of transaction operation.
	 * This is very much a WIP
	 * @param token
	 * @return
	 */
	public String getOperationResult(Token token, int precision)
	{
		//get amount here. will be amount + symbol if appropriate
		if (hasInput())
		{
			decodeTransactionInput(token.getWallet());
			String value = transactionInput.getOperationValue(token, this);
			boolean isSendOrReceive = !from.equalsIgnoreCase(to) && transactionInput.isSendOrReceive(this);
			String prefix = (value.length() == 0 || (value.startsWith("#") || !isSendOrReceive)) ? "" :
					(token.getIsSent(this) ? "- " : "+ ");
			return prefix + value;
		}
		else
		{
			return token.getTransactionValue(this, precision);
		}
	}

	/**
	 * Can the contract call be valid if the operation token is Ethereum?
	 * @param token
	 * @return
	 */
	public boolean shouldShowSymbol(Token token)
	{
		if (hasInput())
		{
			decodeTransactionInput(token.getWallet());
			return transactionInput.shouldShowSymbol(token);
		}
		else
		{
			return true;
		}
	}

	public String getOperationTokenAddress()
	{
		if (hasInput())
		{
			return to;
		}
		else
		{
			return "";
		}
	}

	public String getOperationName(Context ctx, Token token, String walletAddress)
	{
		String txName = null;
		if (isPending())
		{
			txName = ctx.getString(R.string.status_pending);
		}
		else if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			if (token.isEthereum() && shouldShowSymbol(token))
			{
				transactionInput.type = TransactionType.CONTRACT_CALL;
			}

			return transactionInput.getOperationTitle(ctx);
		}

		return txName;
	}

	public boolean hasInput()
	{
		return input != null && input.length() >= 10;
	}

	public int getOperationToFrom(String walletAddress)
	{
		if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			return transactionInput.getOperationToFrom();
		}
		else
		{
			return 0;
		}
	}

	public StatusType getOperationImage(Token token)
	{
		if (hasError())
		{
			return StatusType.FAILED;
		}
		else if (hasInput())
		{
			decodeTransactionInput(token.getWallet());
			return transactionInput.getOperationImage(this, token.getWallet());
		}
		else
		{
			return from.equalsIgnoreCase(token.getWallet()) ? StatusType.SENT : StatusType.RECEIVE;
		}
	}

	public TransactionType getTransactionType(String wallet)
	{
		if (hasError())
		{
			return TransactionType.UNKNOWN;
		}
		else if (hasInput())
		{
			decodeTransactionInput(wallet);
			return transactionInput.type;
		}
		else
		{
			return TransactionType.SEND_ETH;
		}
	}

	/**
	 * Supplimental info in this case is the intrinsic root value attached to a contract call
	 * EG: Calling cryptokitties ERC721 'breedWithAuto' function requires you to call the function and also attach a small amount of ETH
	 * for the 'breeding fee'. That fee is later released to the caller of the 'birth' function.
	 * Supplemental info for these transaction would appear as -0.031 for the 'breedWithAuto' and +0.031 on the 'birth' call
	 * However it's not that simple - the 'breeding fee' will be in the value attached to the transaction, however the 'midwife' reward appears
	 * as an internal transaction, so won't be in the 'value' property.
	 *
	 * @return
	 */
	public String getSupplementalInfo(String walletAddress, String networkName)
	{
		if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			return transactionInput.getSupplimentalInfo(this, walletAddress, networkName);
		}
		else
		{
			return "";
		}
	}

	public String getPrefix(Token token)
	{
		if (hasInput())
		{
			decodeTransactionInput(token.getWallet());
			if (!transactionInput.isSendOrReceive(this) || token.isEthereum())
			{
				return "";
			}
			else if (token.isERC721())
			{
				return "";
			}
		}

		boolean isSent = token.getIsSent(this);
		boolean isSelf = from.equalsIgnoreCase(to);
		if (isSelf) return "";
		else if (isSent) return "- ";
		else return "+ ";
	}

    public BigDecimal getRawValue(String walletAddress) throws Exception
    {
    	if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			return transactionInput.getRawValue();
		}
    	else
		{
			return new BigDecimal(value);
		}
    }

	public StatusType getTransactionStatus()
	{
		if (hasError())
		{
			return StatusType.FAILED;
		}
		else if (blockNumber.equals("-1"))
		{
			return StatusType.REJECTED;
		}
		else if (isPending())
		{
			return StatusType.PENDING;
		}
		else
		{
			return null;
		}
	}

    public void addTransactionElements(Map<String, EventResult> resultMap)
    {
    	resultMap.put("__hash", new EventResult("", hash));
		resultMap.put("__to", new EventResult("", to));
		resultMap.put("__from", new EventResult("", from));
		resultMap.put("__value", new EventResult("", value));
		resultMap.put("__chainId", new EventResult("", String.valueOf(chainId)));
    }

	public String getEventName(String walletAddress)
	{
		String eventName = "";
		if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			eventName = transactionInput.getOperationEvent(walletAddress);
		}

		return eventName;
	}

	public int getSupplementalColour(String supplementalTxt)
	{
		if (!TextUtils.isEmpty(supplementalTxt))
		{
			switch (supplementalTxt.charAt(1))
			{
				case '-':
					return R.color.red;
				case '+':
					return R.color.green;
				default:
					break;
			}
		}

		return R.color.black;
	}

	public String getDestination(Token token)
	{
		if (hasInput())
		{
			decodeTransactionInput(token.getWallet());
			return transactionInput.getOperationAddress(this, token);
		}
		else
		{
			return token.getAddress();
		}
	}

	public String getOperationDetail(Context ctx, Token token, TokensService tService)
	{
		if (hasInput())
		{
			decodeTransactionInput(token.getWallet());
			return transactionInput.getOperationDescription (ctx, this, token, tService);
		}
		else
		{
			return ctx.getString(R.string.operation_definition, ctx.getString(R.string.to), ENSHandler.matchENSOrFormat(ctx, to));
		}
	}

	private void decodeTransactionInput(String walletAddress)
	{
		if (transactionInput == null && hasInput() && Utils.isAddressValid(walletAddress))
		{
			transactionInput = decoder.decodeInput(this, walletAddress);
		}
	}

	public boolean getWalletInvolvedInTransaction(String walletAddr)
	{
		decodeTransactionInput(walletAddr);
		if ((transactionInput != null && transactionInput.functionData != null) && transactionInput.containsAddress(walletAddr)) return true;
		else if (from.equalsIgnoreCase(walletAddr)) return true;
		else if (to.equalsIgnoreCase(walletAddr)) return true;
		else return input != null && input.length() > 40 && input.contains(Numeric.cleanHexPrefix(walletAddr.toLowerCase()));
	}

	public boolean isNFTSent(String walletAddress)
	{
		if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			return transactionInput.isSent();
		}
		else
		{
			return true;
		}
	}

	public boolean getIsSent(String walletAddress)
	{
		if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			return transactionInput.isSent();
		}
		else
		{
			return from.equalsIgnoreCase(walletAddress);
		}
	}

	public boolean isValueChange(String walletAddress)
	{
		if (hasInput())
		{
			decodeTransactionInput(walletAddress);
			return transactionInput.isSendOrReceive(this);
		}
		else
		{
			return true;
		}
	}

	private String calculateContractAddress(String account, long nonce){
		byte[] addressAsBytes = Numeric.hexStringToByteArray(account);
		byte[] calculatedAddressAsBytes =
				Hash.sha3(RlpEncoder.encode(
						new RlpList(
								RlpString.create(addressAsBytes),
								RlpString.create((nonce)))));

		calculatedAddressAsBytes = Arrays.copyOfRange(calculatedAddressAsBytes,
				12, calculatedAddressAsBytes.length);
		return Numeric.toHexString(calculatedAddressAsBytes);
	}
}
