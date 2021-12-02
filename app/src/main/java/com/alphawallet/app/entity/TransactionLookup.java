package com.alphawallet.app.entity;

import com.alphawallet.app.R;

import java.util.HashMap;
import java.util.Map;

public class TransactionLookup
{
    private static final Map<TransactionType, Integer> typeMapping = new HashMap<>();

    public static int typeToName(TransactionType type)
    {
        setupTypes();
        if (type.ordinal() > typeMapping.size())
        {
            return typeMapping.get(TransactionType.UNKNOWN);
        }
        else return typeMapping.get(type);
    }

    public static String typeToEvent(TransactionType type)
    {
        switch (type)
        {
            case TRANSFER_FROM:
            case SEND:
                return "sent";
            case TRANSFER_TO:
                return "received";
            case RECEIVE_FROM:
                return "received";
            case RECEIVED:
                return "received";
            case APPROVE:
                return "ownerApproved";
            default:
                return "";
        }
    }

    public static int toFromText(TransactionType type)
    {
        switch (type)
        {
            case MAGICLINK_PURCHASE:
            case TRANSFER_TO:
                return R.string.to;
            case RECEIVED:
            case RECEIVE_FROM:
                return R.string.from_op;
            case APPROVE:
                return R.string.approve;
            default:
                return R.string.empty;
        }
    }

    private static void setupTypes()
    {
        if (typeMapping.size() == 0)
        {
            typeMapping.put(TransactionType.UNKNOWN, R.string.ticket_invalid_op);
            typeMapping.put(TransactionType.LOAD_NEW_TOKENS, R.string.ticket_load_new_tickets);
            typeMapping.put(TransactionType.MAGICLINK_TRANSFER, R.string.ticket_magiclink_transfer);
            typeMapping.put(TransactionType.MAGICLINK_PICKUP, R.string.ticket_magiclink_pickup);
            typeMapping.put(TransactionType.MAGICLINK_SALE, R.string.ticket_magiclink_sale);
            typeMapping.put(TransactionType.MAGICLINK_PURCHASE, R.string.ticket_magiclink_purchase);
            typeMapping.put(TransactionType.PASS_TO, R.string.ticket_pass_to);
            typeMapping.put(TransactionType.PASS_FROM, R.string.ticket_pass_from);
            typeMapping.put(TransactionType.TRANSFER_TO, R.string.ticket_transfer_to);
            typeMapping.put(TransactionType.RECEIVE_FROM, R.string.ticket_receive_from);
            typeMapping.put(TransactionType.REDEEM, R.string.ticket_redeem);
            typeMapping.put(TransactionType.ADMIN_REDEEM, R.string.ticket_admin_redeem);
            typeMapping.put(TransactionType.CONSTRUCTOR, R.string.ticket_contract_constructor);
            typeMapping.put(TransactionType.TERMINATE_CONTRACT, R.string.ticket_terminate_contract);
            typeMapping.put(TransactionType.TRANSFER_FROM, R.string.ticket_transfer_from);
            typeMapping.put(TransactionType.ALLOCATE_TO, R.string.allocate_to);
            typeMapping.put(TransactionType.APPROVE, R.string.approve);
            typeMapping.put(TransactionType.RECEIVED, R.string.received);
            typeMapping.put(TransactionType.SEND, R.string.action_send);
            typeMapping.put(TransactionType.SEND_ETH, R.string.action_send_eth);
            typeMapping.put(TransactionType.TOKEN_SWAP, R.string.action_token_swap);
            typeMapping.put(TransactionType.WITHDRAW, R.string.action_withdraw);
            typeMapping.put(TransactionType.DEPOSIT, R.string.deposit);
            typeMapping.put(TransactionType.CONTRACT_CALL, R.string.contract_call);
            typeMapping.put(TransactionType.REMIX, R.string.remix);
            typeMapping.put(TransactionType.MINT, R.string.token_mint);
            typeMapping.put(TransactionType.BURN, R.string.token_burn);
            typeMapping.put(TransactionType.COMMIT_NFT, R.string.commit_nft);
            typeMapping.put(TransactionType.SAFE_TRANSFER, R.string.safe_transfer);
            typeMapping.put(TransactionType.SAFE_BATCH_TRANSFER, R.string.safe_batch_transfer);

            typeMapping.put(TransactionType.UNKNOWN_FUNCTION, R.string.contract_call);
        }
    }
}
