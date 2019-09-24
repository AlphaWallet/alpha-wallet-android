package com.alphawallet.app.entity;

import java.util.HashMap;
import java.util.Map;

import com.alphawallet.app.R;

public class TransactionLookup
{
    private static Map<TransactionType, Integer> typeMapping = new HashMap<>();

    public static int typeToName(TransactionType type)
    {
        setupTypes();
        return typeMapping.get(type);
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
            typeMapping.put(TransactionType.UNKNOWN_FUNCTION, R.string.ticket_invalid_op);
        }
    }
}
