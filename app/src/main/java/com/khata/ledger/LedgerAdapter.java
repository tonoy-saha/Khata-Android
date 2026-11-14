package com.khata.ledger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder> {

    public interface Listener {
        void onEdit(Ledger entry);

        void onDelete(Ledger entry);
    }

    private final List<Ledger> entries;
    private final Map<Double, Double> balances;
    private final Context context;
    private final Listener listener;

    public LedgerAdapter(List<Ledger> entries, Map<Double, Double> balances, Context context, Listener listener) {
        this.entries = entries;
        this.balances = balances;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LedgerAdapter.LedgerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ledger, parent, false);
        return new LedgerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LedgerAdapter.LedgerViewHolder holder, int position) {
        final Ledger entry = entries.get(position);
        boolean credit = "credit".equals(entry.getType());
        int color = ContextCompat.getColor(context, credit ? R.color.creditBlue : R.color.debitRed);

        holder.mDate.setText(entry.getDate());
        holder.mType.setText(credit ? R.string.creditBtn : R.string.debitBtn);
        holder.mType.setTextColor(color);
        holder.mParty.setText(entry.getParty());
        holder.mAmount.setText("৳ " + Util.fmt(entry.getAmount()));
        holder.mAmount.setTextColor(color);

        String detail = entry.getCommodity();
        if (!entry.getNote().isEmpty()) {
            detail = detail.isEmpty() ? entry.getNote() : detail + " · " + entry.getNote();
        }
        holder.mDetail.setText(detail);
        holder.mDetail.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);

        Double balance = balances.get(entry.getId());
        holder.mBalance.setText(context.getString(R.string.psBalance) + ": " + Util.fmt(balance == null ? 0 : balance));

        holder.mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEdit(entry);
            }
        });
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDelete(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    public static class LedgerViewHolder extends RecyclerView.ViewHolder {
        TextView mDate, mType, mParty, mAmount, mDetail, mBalance, mEdit, mDelete;

        public LedgerViewHolder(@NonNull View itemView) {
            super(itemView);
            mDate = itemView.findViewById(R.id.tvDate);
            mType = itemView.findViewById(R.id.tvType);
            mParty = itemView.findViewById(R.id.tvParty);
            mAmount = itemView.findViewById(R.id.tvAmount);
            mDetail = itemView.findViewById(R.id.tvDetail);
            mBalance = itemView.findViewById(R.id.tvBalance);
            mEdit = itemView.findViewById(R.id.btnEdit);
            mDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
