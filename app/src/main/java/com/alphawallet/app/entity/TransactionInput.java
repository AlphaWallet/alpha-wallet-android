package com.alphawallet.app.entity;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.token.tools.ParseMagicLink;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.C.BURN_ADDRESS;
import static com.alphawallet.app.C.ETHER_DECIMALS;
import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.ui.widget.holder.TransactionHolder.TRANSACTION_BALANCE_PRECISION;

/**
 * Created by James on 4/03/2018.
 *
 * A data structure that consists part of the transaction: it has the
 * parameters for a function call (currently, only transactions to a
 * contract is dealt in this class) and it is only returned by using
 * TransactionDecoder. Note that the address of the contract, the name
 * of the function called and the signature from transaction sender
 * are all not in this class.
 *
 */

public class TransactionInput
{
    public FunctionData functionData;
    public List<String> addresses;
    public List<BigInteger> arrayValues;
    public List<String> sigData;
    public List<String> miscData;
    public String tradeAddress;
    public TransactionType type;

    private static final String ERC20_APPROVE_ALL = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private static final BigDecimal ERC20_APPROVE_ALL_BD = BigDecimal.valueOf(9.999).movePointRight(17);

    public TransactionInput()
    {
        arrayValues = new ArrayList<>();
        addresses = new ArrayList<>();
        sigData = new ArrayList<>();
        miscData = new ArrayList<>();
    }

    //Addresses are in 256bit format
    public boolean containsAddress(String address)
    {
        boolean hasAddr = false;
        //Scan addresses for this address
        address = Numeric.cleanHexPrefix(address);
        for (String thisAddr : addresses)
        {
            if (thisAddr != null && thisAddr.contains(address))
            {
                hasAddr = true;
                break;
            }
        }

        return hasAddr;
    }

    public String getFirstAddress() {
        String address = "";
        if (addresses.size() > 0)
        {
            address = addresses.get(0);
        }
        return address;
    }

    public String getDestinationAddress()
    {
        String address = "";
        if (addresses.size() > 0)
        {
            address = getFirstAddress();
            switch (functionData.functionName)
            {
                case "transferFrom":
                case "safeTransferFrom":
                case "safeBatchTransferFrom":
                    if (addresses.size() > 1)
                    {
                        address = addresses.get(1); //destination address will be second address
                    }
                    break;
                default:
                    break;
            }
        }
        return address;
    }

    public String getAddress(int index)
    {
        String address = "";
        if (addresses.size() > index)
        {
            address = addresses.get(index);
        }

        return address;
    }

    //With static string as we don't have app context
    String getFirstValue()
    {
        String value = "0";
        if (miscData.size() > 0)
        {
            String firstVal = miscData.get(0);
            if (firstVal.equals(ERC20_APPROVE_ALL))
            {
                value = "All"; //Can't localise here because no App context.
            }
            else
            {
                //convert to big integer
                BigInteger bi = new BigInteger(miscData.get(0), 16);
                value = bi.toString(10);
            }
        }
        return value;
    }

    private String getFirstValueScaled(Context ctx, int tokenDecimal)
    {
        String value = "0";
        if (miscData.size() > 0)
        {
            String firstVal = miscData.get(0);
            BigInteger bi = new BigInteger(firstVal, 16);
            BigDecimal scaledVal = new BigDecimal(bi).divide(BigDecimal.valueOf(Math.pow(10, tokenDecimal)), 18, RoundingMode.DOWN);
            if (firstVal.equals(ERC20_APPROVE_ALL) || scaledVal.compareTo(ERC20_APPROVE_ALL_BD) >= 0)
            {
                if (ctx != null)
                {
                    value = ctx.getString(R.string.all);
                }
                else
                {
                    value = "All";
                }
            }
            else
            {
                //convert to big integer
                value = BalanceUtils.getScaledValueMinimal(new BigDecimal(bi),
                        tokenDecimal, 4);
            }
        }
        return value;
    }

    public String getOperationTitle(Context ctx)
    {
        return ctx.getString(TransactionLookup.typeToName(type));
    }

    public int getOperationToFrom()
    {
        switch (type)
        {
            case MAGICLINK_SALE:
            case MAGICLINK_TRANSFER:
            case TRANSFER_TO:
            case SEND:
                return R.string.to;
            case RECEIVED:
            case RECEIVE_FROM:
            case MAGICLINK_PURCHASE:
            case MAGICLINK_PICKUP:
                return R.string.from_op;
            case APPROVE:
                return R.string.approve;
            default:
                return 0;
        }
    }

    public String getOperationDescription(Context ctx, Transaction tx, Token t, TokensService tService)
    {
        String operation = "";
        switch (type)
        {
            case APPROVE: //show address as token name, ENS name or formatted address
                String amount = getOperationValue(t, tx);
                String approveAddr = getFirstAddress();
                Token approveToken = tService.getToken(tx.chainId, approveAddr);
                approveAddr = approveToken == null ? ENSHandler.matchENSOrFormat(ctx, approveAddr) : approveToken.getShortName();
                operation = ctx.getString(R.string.default_approve, amount, approveAddr);
                break;
            case TERMINATE_CONTRACT:
                operation = ENSHandler.matchENSOrFormat(ctx, tx.to);
                break;
            case CONSTRUCTOR:
                operation = t.getFullName();
                break;
            default: //use default definition
                int operationResId = getOperationToFrom();
                if (operationResId != 0)
                {
                    operation = ctx.getString(R.string.operation_definition, ctx.getString(operationResId), ENSHandler.matchENSOrFormat(ctx, getOperationAddress(tx, t)));
                }
                else
                {
                    operation = ENSHandler.matchENSOrFormat(ctx, tx.to);
                }
                break;
        }

        return operation;
    }

    public String getOperationAddress(Transaction tx, Token t)
    {
        String address = tx.to;

        switch (type)
        {
            case MAGICLINK_TRANSFER: //transferred out of our wallet via magic link (to ->)
                address = tx.from;
                break;
            case MAGICLINK_SALE: //we received ether from magiclink sale (to ->)
                address = tx.from;
                break;
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink (from ->)
                address = tradeAddress;
                break;
            case MAGICLINK_PICKUP: //received ticket from a magic link (from ->)
                address = tradeAddress;
                break;
            case PASS_TO: //we had a ticket transferred out of our wallet paid for by server. (to -> )
                address = getDestinationAddress();
                break;
            case PASS_FROM: //we received a ticket from magiclink with transfer paid by server
                address = tx.from; //(from ->)
                break;
            case REDEEM:
            case ADMIN_REDEEM:
                address = BURN_ADDRESS;
                break;
            case TRANSFER_TO:
                break;
            case SEND:
                address = getDestinationAddress();
                break;
            case RECEIVE_FROM:
                break;
            case TRANSFER_FROM:
            case SAFE_TRANSFER:
                address = tx.from;
                break;
            case SAFE_BATCH_TRANSFER:
                address = tx.to.equalsIgnoreCase(t.getWallet()) ? tx.from : tx.to;
                break;
            case REMIX:
            case COMMIT_NFT:
                break;
            case BURN:
            case MINT:
                address = t.getFullName();
                break;
            case ALLOCATE_TO:
                address = getFirstAddress();
                break;
            case APPROVE:
                address = getFirstAddress();
                break;
            case RECEIVED:
                address = tx.from;
                break;
            case LOAD_NEW_TOKENS:
            case CONSTRUCTOR:
            case TERMINATE_CONTRACT:
                address = t.getAddress();
                break;
            case UNKNOWN_FUNCTION:
            case INVALID_OPERATION:
            default:
                address = tx.from;
                break;
        }

        return address;
    }

    private String getMagicLinkAddress(Transaction tx)
    {
        String address = tx.from;
        if (!tx.error.equals("0")) return address;
        try
        {
            Sign.SignatureData sig = Transaction.decoder.getSignatureData(this);
            //ecrecover the recipient of the ether
            int[] ticketIndexArray = Transaction.decoder.getIndices(this);
            String expiryStr = miscData.get(0);
            long expiry = Long.valueOf(expiryStr, 16);
            BigInteger priceWei = new BigInteger(tx.value);
            String contractAddress = tx.to;

            ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains()); //parser on demand
            byte[] tradeBytes = parser.getTradeBytes(ticketIndexArray, contractAddress, priceWei, expiry);
            //attempt ecrecover
            BigInteger key = Sign.signedMessageToKey(tradeBytes, sig);
            address = "0x" + Keys.getAddress(key);
        }
        catch (Exception e)
        {
            address = tx.from;
        }

        return address;
    }

    public void setOperationType(Transaction tx, String walletAddress)
    {
        if (tx != null && (tx.isConstructor ||
                (tx.input != null && tx.input.equals(Transaction.CONSTRUCTOR)))) //input labelled as constructor
        {
            type = TransactionType.CONSTRUCTOR;
            return;
        }

        if (tx != null && tx.input != null && tx.input.equals("0x"))
        {
            type = TransactionType.SEND_ETH;
            return;
        }

        switch (functionData.functionName)
        {
            case "trade":
                type = interpretTradeData(tx, walletAddress);
                break;
            case "safeTransferFrom":
                type = interpretSafeTransferFrom(walletAddress);
                break;
            case "safeBatchTransferFrom":
                type = TransactionType.SAFE_BATCH_TRANSFER;
                break;
            case "transferFrom":
                type = interpretTransferFrom(walletAddress);
                break;
            case "transfer":
                type = interpretTransfer(walletAddress);
                break;
            case "allocateTo":
                type = TransactionType.ALLOCATE_TO;
                break;
            case "approve":
                type = TransactionType.APPROVE;
                break;
            case "loadNewTickets":
                type = TransactionType.LOAD_NEW_TOKENS;
                break;
            case "passTo":
                type = interpretPassTo(tx, walletAddress);
                break;
            case "endContract":
            case "selfdestruct":
            case "kill":
                type = TransactionType.TERMINATE_CONTRACT;
                break;
            case "swapExactTokensForTokens":
                type = TransactionType.TOKEN_SWAP;
                break;
            case "withdraw":
                type = TransactionType.WITHDRAW;
                break;
            case "deposit":
                type = TransactionType.DEPOSIT;
                break;
            case "remix":
                type = TransactionType.REMIX;
                break;
            case "mint":
                type = TransactionType.MINT;
                break;
            case "burn":
                type = TransactionType.BURN;
                break;
            case "commitNFT":
                type = TransactionType.COMMIT_NFT;
                break;
            default:
                type = TransactionType.CONTRACT_CALL;
                break;
        }
    }

    private TransactionType interpretTradeData(Transaction tx, String walletAddr)
    {
        if (tx == null)
        {
            return TransactionType.MAGICLINK_TRANSFER;
        }

        BigInteger priceWei = new BigInteger(tx.value);
        tradeAddress = getMagicLinkAddress(tx);
        if (priceWei.equals(BigInteger.ZERO))
        {
            if (tradeAddress.equalsIgnoreCase(walletAddr))
            {
                //transfered out of our wallet via magic link
                return TransactionType.MAGICLINK_TRANSFER;// R.string.ticket_magiclink_transfer;
            }
            else
            {
                //received ticket from a magic link
                return TransactionType.MAGICLINK_PICKUP;// R.string.ticket_magiclink_pickup;
            }
        }
        else
        {
            if (tradeAddress.equalsIgnoreCase(walletAddr))
            {
                //we received ether from magiclink sale
                return TransactionType.MAGICLINK_SALE;// R.string.ticket_magiclink_sale;
            }
            else
            {
                //we purchased a ticket from a magiclink
                return TransactionType.MAGICLINK_PURCHASE;// R.string.ticket_magiclink_purchase;
            }
        }
    }

    private TransactionType interpretPassTo(Transaction tx, String walletAddr)
    {
        if (tx == null)
        {
            return TransactionType.PASS_TO;
        }

        tradeAddress = getMagicLinkAddress(tx);
        if (tradeAddress.equalsIgnoreCase(walletAddr))
        {
            return TransactionType.PASS_FROM;
        }
        else
        {
            return TransactionType.PASS_TO;
        }
    }

    private TransactionType interpretSafeTransferFrom(String walletAddr)
    {
        String destinationAddr = getDestinationAddress();
        String fromAddr = getFirstAddress();
        if (walletAddr == null)
        {
            return TransactionType.SAFE_TRANSFER;
        }
        else if (destinationAddr.equals(C.BURN_ADDRESS))
        {
            return TransactionType.BURN;
        }
        else if (fromAddr.equals(BURN_ADDRESS))
        {
            return TransactionType.MINT;
        }
        else if (!destinationAddr.equalsIgnoreCase(walletAddr)) //otherparty in this case will be the first address, the previous owner of the token(s)
        {
            return TransactionType.SAFE_TRANSFER;
        }
        else
        {
            return TransactionType.TRANSFER_TO;
        }
    }

    private TransactionType interpretTransferFrom(String walletAddr)
    {
        String destinationAddr = getDestinationAddress();
        String fromAddr = getFirstAddress();
        if (walletAddr == null)
        {
            return TransactionType.TRANSFER_FROM;
        }
        else if (destinationAddr.equals(C.BURN_ADDRESS))
        {
            return TransactionType.REDEEM;
        }
        else if (!destinationAddr.equalsIgnoreCase(walletAddr)) //otherparty in this case will be the first address, the previous owner of the token(s)
        {
            return TransactionType.TRANSFER_FROM;
        }
        else
        {
            return TransactionType.TRANSFER_TO;
        }
    }

    private TransactionType interpretTransfer(String walletAddr)
    {
        if (walletAddr == null)
        {
            return TransactionType.TRANSFER_TO;
        }
        //this could be transfer to or from
        //if destination is our address then it must be a receive
        else if (getDestinationAddress().equalsIgnoreCase(walletAddr))
        {
            return TransactionType.RECEIVED;// R.string.ticket_receive_from;
        }
        else
        {
            return TransactionType.SEND;
        }
    }

    public String getSupplimentalInfo(Transaction tx, String walletAddress, String networkName)
    {
        String supplimentalTxt = "";
        switch (type)
        {
            case MAGICLINK_SALE: //we received ether from magiclink sale
                supplimentalTxt = "(+" + BalanceUtils.getScaledValue(tx.value, ETHER_DECIMALS) + " " + networkName + ")";
                break;
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink
                supplimentalTxt = "(-" + BalanceUtils.getScaledValue(tx.value, ETHER_DECIMALS) + " " + networkName + ")";
                break;
            default:
                break;
        }

        return supplimentalTxt;
    }

    public String getOperationValue(Token token, Transaction tx)
    {
        String operationValue = "";

        boolean addSymbol = true;

        switch (type)
        {
            case PASS_TO:   //we had a ticket transferred out of our wallet paid for by server.
            case PASS_FROM: //we received a ticket from magiclink with transfer paid by server
            case REDEEM:
            case ADMIN_REDEEM:
            case MAGICLINK_TRANSFER: //transferred out of our wallet via magic link (to ->)
            case MAGICLINK_SALE: //we received ether from magiclink sale (to ->)
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink (from ->)
            case MAGICLINK_PICKUP: //received ticket from a magic link (from ->)
                operationValue = String.valueOf(arrayValues.size());
                break;
            case APPROVE:
            case ALLOCATE_TO:
                operationValue = getApproveValue(token);
                if (token.isNonFungible()) addSymbol = false;
                break;
            case TRANSFER_TO:
            case RECEIVE_FROM:
            case TRANSFER_FROM:
            case RECEIVED:
            case SEND:
                operationValue = token.getTransferValue(tx.transactionInput, TRANSACTION_BALANCE_PRECISION);
                break;
            case BURN:
            case MINT:
                operationValue = token.getTransferValue(tx.transactionInput, TRANSACTION_BALANCE_PRECISION);
                break;

            case REMIX:
            case COMMIT_NFT:
                operationValue = "";
                addSymbol = false;
                break;

            case LOAD_NEW_TOKENS:
                operationValue = String.valueOf(arrayValues.size());
                break;
            case CONSTRUCTOR:
            case TERMINATE_CONTRACT:
                break;
            case CONTRACT_CALL:
                addSymbol = false;
                break;
            case TOKEN_SWAP:
                addSymbol = false;
                break;
            case WITHDRAW:
                addSymbol = false;
                break;
            case DEPOSIT:
                //base value of tx
                operationValue = token.getTransactionValue(tx, TRANSACTION_BALANCE_PRECISION);// .getTransferValue(tx.transactionInput, TRANSACTION_BALANCE_PRECISION);
                addSymbol = true;
                break;
            case UNKNOWN_FUNCTION:
            case INVALID_OPERATION:
            default:
                break;
        }

        if (addSymbol)
        {
            if (!token.isEthereum())
            {
                operationValue = operationValue + " " + token.getSymbol();
            }
            else if (!TextUtils.isEmpty(operationValue))
            {
                operationValue = operationValue + " " + token.getSymbol();
            }
            else
            {
                operationValue = "";
            }
        }

        return operationValue;
    }

    private String getApproveValue(Token token)
    {
        if (token.isNonFungible() && miscData.size() > 0)
        {
            //grab tokenId
            BigInteger bi = new BigInteger(miscData.get(0), 16);
            return "#" + bi.toString(10);
        }
        else
        {
            return getFirstValueScaled(null, token.tokenInfo.decimals);
        }
    }

    public boolean shouldShowSymbol(Token token)
    {
        switch (type)
        {
            case CONTRACT_CALL:
            case TOKEN_SWAP:
            case WITHDRAW:
                return false;
            default:
                return !token.isEthereum(); //can't be ethereum if has transaction input
        }
    }

    public StatusType getOperationImage(Transaction tx, String walletAddress)
    {
        switch (type)
        {
            case PASS_TO:
            case SEND:
                return StatusType.SENT;
            case PASS_FROM:
                return StatusType.RECEIVE;
            case REDEEM:
            case ADMIN_REDEEM:
                return StatusType.SENT;
            case MAGICLINK_TRANSFER: //transferred out of our wallet via magic link (to ->)
                return StatusType.SENT;
            case MAGICLINK_SALE: //we received ether from magiclink sale (to ->)
                return StatusType.SENT;
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink (from ->)
                return StatusType.RECEIVE;
            case MAGICLINK_PICKUP: //received ticket from a magic link (from ->)
                return StatusType.RECEIVE;
            case TRANSFER_TO:
                return StatusType.RECEIVE;
            case RECEIVE_FROM:
                return StatusType.SENT;
            case TRANSFER_FROM:
                return StatusType.SENT;
            case APPROVE:
                return StatusType.SENT;
            case RECEIVED:
                return StatusType.RECEIVE;
            case ALLOCATE_TO:
                return StatusType.SENT;
            case BURN:
                return StatusType.SENT;
            case MINT:
                return StatusType.RECEIVE;
            case REMIX:
                return StatusType.NONE;
            case LOAD_NEW_TOKENS:
            case CONSTRUCTOR:
            case TERMINATE_CONTRACT:
                return StatusType.CONSTRUCTOR;
            default:
                return tx.from.equalsIgnoreCase(walletAddress) ? (tx.to.equalsIgnoreCase(tx.from) ? StatusType.SELF : StatusType.SENT)
                    : StatusType.RECEIVE;
        }
    }

    public BigDecimal getRawValue()
    {
        //this is for a transfer etc.
        if (arrayValues.size() > 0)
        {
            return new BigDecimal(arrayValues.size());
        }
        else
        {
            //get first value
            return new BigDecimal(getFirstValue());
        }
    }

    public String getOperationEvent(String walletAddress)
    {
        switch (type)
        {
            case SEND:
            case TRANSFER_FROM:
            case BURN:
                return "sent";
            case TRANSFER_TO:
                return "received";
            case RECEIVE_FROM:
            case MINT:
            case RECEIVED:
                return "received";
            case APPROVE:
                return "ownerApproved";
            default:
                if (getDestinationAddress().equalsIgnoreCase(walletAddress))
                {
                    return "received";
                }
                else
                {
                    return "sent";
                }
        }
    }

    public boolean isSent()
    {
        switch (type)
        {
            case SEND:
            case PASS_TO:
                return true;
            case PASS_FROM:
                return false;
            case REDEEM:
            case ADMIN_REDEEM:
                return true;
            case MAGICLINK_TRANSFER: //transferred out of our wallet via magic link (to ->)
                return true;
            case MAGICLINK_SALE: //we received ether from magiclink sale (to ->)
                return true;
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink (from ->)
                return false;
            case MAGICLINK_PICKUP: //received ticket from a magic link (from ->)
                return false;
            case TRANSFER_TO:
            case MINT:
                return false;
            case RECEIVE_FROM:
                return true;
            case TRANSFER_FROM:
            case BURN:
                return true;
            case APPROVE:
                return true;
            case RECEIVED:
                return false;
            case ALLOCATE_TO:
                return true;
            default:
                return true;
        }
    }

    public boolean isSendOrReceive(Transaction tx)
    {
        switch (type)
        {
            case SEND:
            case PASS_TO:
            case PASS_FROM:
            case REDEEM:
            case ADMIN_REDEEM:
            case MAGICLINK_TRANSFER: //transferred out of our wallet via magic link (to ->)
            case MAGICLINK_SALE: //we received ether from magiclink sale (to ->)
            case MAGICLINK_PURCHASE: //we purchased a ticket from a magiclink (from ->)
            case MAGICLINK_PICKUP: //received ticket from a magic link (from ->)
            case TRANSFER_TO:
            case RECEIVE_FROM:
            case MINT:
            case BURN:
            case TRANSFER_FROM:
            case RECEIVED:
            case REMIX:
                return true;
            default:
                return !tx.value.equals("0");
        }
    }
}
