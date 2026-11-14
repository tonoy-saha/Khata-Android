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

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface Listener {
        void onEdit(Expense expense);

        void onDelete(Expense expense);
    }

    private final List<Expense> expenses;
    private final Context context;
    private final Listener listener;

    public ExpenseAdapter(List<Expense> expenses, Context context, Listener listener) {
        this.expenses = expenses;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseAdapter.ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseAdapter.ExpenseViewHolder holder, int position) {
        final Expense expense = expenses.get(position);
        boolean debit = "debit".equals(expense.getType());
        int color = ContextCompat.getColor(context, debit ? R.color.debitRed : R.color.creditBlue);

        holder.mDate.setText(expense.getDate());
        holder.mType.setText(debit ? R.string.debitWord : R.string.creditWord);
        holder.mType.setTextColor(color);
        holder.mCategory.setText(Util.catLabel(context, expense.getCategory()));
        holder.mAmount.setText("৳ " + Util.fmt(expense.getAmount()));
        holder.mAmount.setTextColor(color);
        holder.mNote.setText(expense.getNote());
        holder.mNote.setVisibility(expense.getNote().isEmpty() ? View.GONE : View.VISIBLE);

        holder.mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEdit(expense);
            }
        });
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDelete(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenses == null ? 0 : expenses.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView mDate, mType, mCategory, mAmount, mNote, mEdit, mDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            mDate = itemView.findViewById(R.id.tvDate);
            mType = itemView.findViewById(R.id.tvType);
            mCategory = itemView.findViewById(R.id.tvCategory);
            mAmount = itemView.findViewById(R.id.tvAmount);
            mNote = itemView.findViewById(R.id.tvNote);
            mEdit = itemView.findViewById(R.id.btnEdit);
            mDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
