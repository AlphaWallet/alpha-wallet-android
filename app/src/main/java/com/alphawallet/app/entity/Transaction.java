package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.VelasUtils;
import com.alphawallet.token.tools.ParseMagicLink;
import com.google.gson.annotations.SerializedName;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

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
public class Transaction implements Parcelable {
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
    public final TransactionOperation[] operations;
    public final String error;
    public final int chainId;

    public boolean isConstructor = false;

	private static TransactionDecoder decoder = null;
	private static ParseMagicLink parser = null;

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
            int chainId,
            TransactionOperation[] operations) {
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
		this.operations = operations;
	}

	public Transaction(String hash, String isError, String blockNumber, long timeStamp, int nonce, String from, String to,
					   String value, String gas, String gasPrice, String input, String gasUsed, int chainId, String contractAddress)
	{
		//build transaction using input
		TransactionInput f;

		if (!TextUtils.isEmpty(contractAddress)) //must be a constructor
		{
			if (decoder == null) decoder = new TransactionDecoder(); //initialise decoder on demand
			to = contractAddress;
			//add a constructor here
			operations = generateERC875Op();
			TransactionContract ct = operations[0].contract;
			ct.setOperation(TransactionType.CONSTRUCTOR);
			ct.address = contractAddress;
			ct.setType(-3);// indicate that we need to load the contract
			operations[0].value = "";
			isConstructor = true;
			ContractType type = decoder.getContractType(input);
			ct.decimals = type.ordinal();
			input = "Constructor"; //Placeholder - don't consume storage for the constructor
		}
		else
		{
			//Now perform as complete processing as we are able to here. This saves re-allocating and makes code far less brittle.
			TransactionOperation[] o = new TransactionOperation[0];

			//TODO: Handle transaction with multiple operations
			if (input != null && input.length() >= 10)
			{
				TransactionOperation op = null;
				TransactionContract ct;

				if (decoder == null) decoder = new TransactionDecoder(); //initialise decoder on demand

				f = decoder.decodeInput(input);
				//is this a trade?
				if (f.functionData != null)
				{
					//recover recipient
					switch (f.functionData.functionFullName)
					{
						case "trade(uint256,uint16[],uint8,bytes32,bytes32)":
						case "trade(uint256,uint256[],uint8,bytes32,bytes32)":
							o = processTrade(f, contractAddress);
							op = o[0];
							setName(o, TransactionType.MAGICLINK_TRANSFER);
							op.contract.address = to;
							op.value = String.valueOf(f.paramValues.size());
							break;
						case "transferFrom(address,address,uint16[])":
						case "transferFrom(address,address,uint256[])":
							o = generateERC875Op();
							op = o[0];
							op.contract.setIndicies(f.paramValues);
							if (f.containsAddress(C.BURN_ADDRESS))
							{
								setName(o, TransactionType.REDEEM);
							}
							else
							{
								setName(o, TransactionType.TRANSFER_FROM);
							}
							op.contract.setType(-1);
							op.contract.address = to;
							op.contract.setOtherParty(f.getFirstAddress());
							op.value = String.valueOf(f.paramValues.size());
							op.to = f.getAddress(1);
							break;
						case "transfer(address,uint16[])":
						case "transfer(address,uint256[])":
							o = generateERC875Op();
							op = o[0];
							op.contract.setOtherParty(f.getFirstAddress());
							op.contract.setIndicies(f.paramValues);
							setName(o, TransactionType.TRANSFER_TO);
							op.value = String.valueOf(f.paramValues.size());
							op.contract.address = to;
							break;
						case "transfer(address,uint256)":
							o = generateERC20Op();
							op = o[0];
							op.from = from;
							op.to = f.getFirstAddress();
							op.value = String.valueOf(f.getFirstValue());
							op.contract.address = to;
							setName(o, TransactionType.TRANSFER_TO);
							break;
						case "transferFrom(address,address,uint256)":
							o = generateERC20Op();
							op = o[0];
							op.from = f.getFirstAddress();
							op.to = f.getAddress(1);
							op.value = String.valueOf(f.getFirstValue());
							op.contract.address = to;
							setName(o, TransactionType.TRANSFER_FROM);
							op.contract.setType(1);
							break;
						case "allocateTo(address,uint256)":
							o = generateERC20Op();
							op = o[0];
							op.from = from;
							op.to = f.getFirstAddress();
							op.value = String.valueOf(f.getFirstValue());
							op.contract.address = to;
							setName(o, TransactionType.ALLOCATE_TO);
							break;
						case "approve(address,uint256)":
							o = generateERC20Op();
							op = o[0];
							op.from = from;
							op.to = f.getFirstAddress();
							op.value = String.valueOf(f.getFirstValue());
							op.contract.address = to;
							setName(o, TransactionType.APPROVE);
							break;
						case "loadNewTickets(bytes32[])":
						case "loadNewTickets(uint256[])":
							o = generateERC875Op();
							op = o[0];
							op.from = from;
							op.value = String.valueOf(f.paramValues.size());
							op.contract.address = to;
							setName(o, TransactionType.LOAD_NEW_TOKENS);
							op.contract.setType(1);
							break;
						case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
						case "passTo(uint256,uint256[],uint8,bytes32,bytes32,address)":
							o = processPassTo(f, contractAddress);
							op = o[0];
							op.from = from;
							op.to = f.getFirstAddress();
							op.value = String.valueOf(f.paramValues.size());
							op.contract.address = to;
							setName(o, TransactionType.PASS_TO);
							op.contract.setType(-1);
							break;
						case "endContract()":
						case "selfdestruct()":
						case "kill()":
							o = generateERC875Op();
							op = o[0];
							ct = op.contract;
							ct.setOperation(TransactionType.TERMINATE_CONTRACT);
							ct.name = to;
							ct.setType(-2);
							setName(o, TransactionType.TERMINATE_CONTRACT);
							op.value = "";
							ct.address = to;
							break;
						default:
							break;
					}

					if (op != null)
					{
						op.transactionId = hash;
					}
				}
			}
			operations = o;
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

	public String getTokenAddress(String walletAddress)
	{
		if (operations == null || operations.length == 0)
		{
			return walletAddress;
		}
		else return to;
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
		chainId = in.readInt();
		Parcelable[] parcelableArray = in.readParcelableArray(TransactionOperation.class.getClassLoader());
		TransactionOperation[] operations = null;
		if (parcelableArray != null)
		{
			operations = Arrays.copyOf(parcelableArray, parcelableArray.length, TransactionOperation[].class);
		}
		this.operations = operations;
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
		dest.writeInt(chainId);
		dest.writeParcelableArray(operations, flags);
	}

	public boolean isRelated(String contractAddress, String walletAddress)
	{
		TransactionOperation operation = operations == null
				|| operations.length == 0 ? null : operations[0];

		if (contractAddress.equals("eth"))
		{
			return (input.equals("0x") || from.equalsIgnoreCase(walletAddress));
		}
		else if (walletAddress.equalsIgnoreCase(contractAddress)) //transactions sent from or sent to the main currency account
		{
			return from.equalsIgnoreCase(walletAddress) || to.equalsIgnoreCase(walletAddress);
		}
		else
		{
			if (to.equalsIgnoreCase(contractAddress)) return true;
			else return operation != null && (operations[0].contract.address.equalsIgnoreCase(contractAddress));
		}
	}

    public TransactionContract getOperation()
    {
		return operations == null
				|| operations.length == 0 ? null : operations[0].contract;
    }

	public String vlxFromAddress() {
		if (!TextUtils.isEmpty(from) && Hex.containsHexPrefix(from)) {
			return VelasUtils.ethToVlx(from);
		}
		return from;
	}

	public String vlxToAddress() {
		if (!TextUtils.isEmpty(to) && Hex.containsHexPrefix(to)) {
			return VelasUtils.ethToVlx(to);
		}
		return to;
	}

	private TransactionOperation[] generateERC20Op()
	{
		TransactionOperation[] o = new TransactionOperation[1];
		TransactionOperation op = new TransactionOperation();
		TransactionContract ct = new TransactionContract();
		o[0] = op;
		op.contract = ct;
		return o;
	}

	private TransactionOperation[] generateERC875Op()
	{
		TransactionOperation[] o = new TransactionOperation[1];
		TransactionOperation op = new TransactionOperation();
		ERC875ContractTransaction ct = new ERC875ContractTransaction();
		o[0] = op;
		op.contract = ct;
		return o;
	}

	private TransactionOperation[] processPassTo(TransactionInput f, String contractAddress)
	{
		TransactionOperation[] o = processTrade(f, contractAddress);
		if (o.length > 0)
		{
			o[0].contract.totalSupply = f.getFirstAddress(); //store destination address for this passTo. We don't use totalSupply for anything else in this case
		}

		return o;
	}

	private TransactionOperation[] processTrade(TransactionInput f, String contractAddress)
	{
		TransactionOperation[] o;
		try
		{
			Sign.SignatureData sig = decoder.getSignatureData(f);
			//ecrecover the recipient of the ether
			int[] ticketIndexArray = decoder.getIndices(f);
			String expiryStr = f.miscData.get(0);
			long expiry = Long.valueOf(expiryStr, 16);
			BigInteger priceWei = new BigInteger(value);
			contractAddress = to;
			o = generateERC875Op();
			TransactionOperation op = o[0];
			TransactionContract ct = op.contract;
			if (error.equals("0")) //don't bother checking signature unless the transaction succeeded
			{
				if (parser == null) parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains()); //parser on demand
				byte[] tradeBytes = parser.getTradeBytes(ticketIndexArray, contractAddress, priceWei, expiry);
				//attempt ecrecover
				BigInteger key = Sign.signedMessageToKey(tradeBytes, sig);
				ct.setOtherParty("0x" + Keys.getAddress(key));
			}
			ct.address = contractAddress;
			ct.setIndicies(f.paramValues);
			ct.name = contractAddress;
		}
		catch (Exception e)
		{
			o = generateERC875Op();
			//e.printStackTrace();
		}

		return o;
	}

	private void setName(TransactionOperation[] o, TransactionType name)
	{
		if (o.length > 0 && o[0] != null)
		{
			TransactionOperation op = o[0];
			TransactionContract ct = op.contract;
			if (ct instanceof ERC875ContractTransaction)
			{
				((ERC875ContractTransaction) ct).operation = name;
			}
			else
			{
				op.contract.name = "*" + String.valueOf(name.ordinal());
			}
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
		if (operations == null || operations.length == 0)
			return token.getTransactionValue(this, precision);
		if (error.equals("1")) return "";

		//TODO: Handle multiple operation transactions
		TransactionOperation operation = operations[0];

		return operation.getOperationResult(token, this);
	}

	public String getOperationTokenAddress()
	{
		TransactionOperation operation = operations == null
												 || operations.length == 0 ? null : operations[0];

		if (operation == null || operation.contract == null)
		{
			return "";
		}
		else
		{
			return operation.contract.address;
		}
	}

	public String getOperationName(Context ctx)
	{
		String txName = null;
		try
		{
			if (blockNumber != null && blockNumber.equals("0"))
			{
				txName = ctx.getString(R.string.status_pending);
			}
			else if (operations != null && operations.length > 0)
			{
				TransactionOperation operation = operations[0];
				txName = operation.getOperationName(ctx);
			}
		}
		catch (NumberFormatException e)
		{
			//Silent fail, number was invalid just display default
		}

		return txName;
	}

	public String getContract(Token token)
	{
		TransactionOperation operation = operations == null
												 || operations.length == 0 ? null : operations[0];

		if (operation == null || operation.contract == null)
		{
			return token.getAddress();
		}
		else
		{
			return token.getFullName();
		}
	}

	public StatusType getOperationImage(Token token)
	{
		TransactionOperation operation = operations == null
												 || operations.length == 0 ? null : operations[0];

		if (operation == null || operation.contract == null)
		{
			return from.equalsIgnoreCase(token.getWallet()) ? StatusType.SENT : StatusType.RECEIVE;
		}
		else
		{
			return operation.contract.getOperationImage(token, this);
		}
	}

	/**
	 * Supplimental info in this case is the intrinsic root value attached to a contract call
	 * EG: Calling cryptokitties ERC721 'breedWithAuto' function requires you to call the function and also attach a small amount of ETH
	 * for the 'breeding fee'. That fee is later released to the caller of the 'birth' function.
	 * Supplimental info for these transaction would appear as -0.031 for the 'breedWithAuto' and +0.031 on the 'birth' call
	 * However it's not that simple - the 'breeding fee' will be in the value attached to the transaction, however the 'midwife' reward appears
	 * as an internal transaction, so won't be in the 'value' property.
	 *
	 * @return
	 */
	public String getSupplementalInfo(String walletAddress, String networkName)
	{
		TransactionOperation operation = operations == null
												 || operations.length == 0 ? null : operations[0];

		if (operation == null || operation.contract == null)
		{
			return "";
		}
		else
		{
			return operation.contract.getSupplimentalInfo(this, walletAddress, networkName);
		}
	}

	public String getPrefix(Token token)
	{
		boolean isSent = token.getIsSent(this);
		boolean isSelf = from.equalsIgnoreCase(to);
		if (isSelf) return "";
		else if (isSent) return "-";
		else return "+";
	}

    public BigDecimal getRawValue() throws Exception
    {
		if (operations == null || operations.length == 0)
		{
			return new BigDecimal(value);
		}
		else
		{
			TransactionOperation operation = operations[0];
			return operation.getRawValue();
		}
    }

	public StatusType getTransactionStatus()
	{
		if (error != null && error.equals("1"))
		{
			return StatusType.FAILED;
		}
		else if (blockNumber.equals("-1"))
		{
			return StatusType.REJECTED;
		}
		else if (blockNumber.equals("0"))
		{
			return StatusType.PENDING;
		}
		else
		{
			return null;
		}
	}

	public void completeSetup(String walletAddress)
	{
		if (operations.length > 0)
		{
			TransactionOperation op = operations[0];
			if (op.contract != null) op.contract.completeSetup(walletAddress.toLowerCase(), this);
		}
	}

    public void addTransactionElements(Map<String, RealmAuxData.EventResult> resultMap)
    {
    	resultMap.put("__hash", new RealmAuxData.EventResult("", hash));
		resultMap.put("__to", new RealmAuxData.EventResult("", to));
		resultMap.put("__from", new RealmAuxData.EventResult("", from));
		resultMap.put("__value", new RealmAuxData.EventResult("", value));
		resultMap.put("__chainId", new RealmAuxData.EventResult("", String.valueOf(chainId)));
    }
}
