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

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    public interface Listener {
        void onDelete(Stock stock);
    }

    private final List<Stock> stocks;
    private final Context context;
    private final Listener listener;

    public StockAdapter(List<Stock> stocks, Context context, Listener listener) {
        this.stocks = stocks;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StockAdapter.StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockAdapter.StockViewHolder holder, int position) {
        final Stock stock = stocks.get(position);
        holder.mDate.setText(stock.getDate());
        holder.mCommodity.setText(stock.getCommodity());
        String sell = stock.getSellPrice() != null ? Util.fmt(stock.getSellPrice()) : "-";
        holder.mLine.setText(context.getString(R.string.thBuy) + " " + Util.fmt(stock.getBuyPrice())
                + "   " + context.getString(R.string.thSell) + " " + sell
                + "   " + context.getString(R.string.thQty) + " " + Util.fmt(stock.getQuantity()));

        if (stock.getSellPrice() != null) {
            double profit = (stock.getSellPrice() - stock.getBuyPrice()) * stock.getQuantity();
            holder.mProfit.setText(context.getString(R.string.thProfit) + ": " + Util.fmt(profit));
            holder.mProfit.setTextColor(ContextCompat.getColor(context, profit >= 0 ? R.color.creditBlue : R.color.debitRed));
            holder.mProfit.setVisibility(View.VISIBLE);
        } else {
            holder.mProfit.setVisibility(View.GONE);
        }
        holder.mNote.setText(stock.getNote());
        holder.mNote.setVisibility(stock.getNote().isEmpty() ? View.GONE : View.VISIBLE);

        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDelete(stock);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stocks == null ? 0 : stocks.size();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView mDate, mCommodity, mLine, mProfit, mNote, mDelete;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            mDate = itemView.findViewById(R.id.tvDate);
            mCommodity = itemView.findViewById(R.id.tvCommodity);
            mLine = itemView.findViewById(R.id.tvLine);
            mProfit = itemView.findViewById(R.id.tvProfit);
            mNote = itemView.findViewById(R.id.tvNote);
            mDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
