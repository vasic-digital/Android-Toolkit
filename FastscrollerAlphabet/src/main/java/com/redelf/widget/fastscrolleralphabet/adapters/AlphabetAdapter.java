/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.widget.fastscrolleralphabet.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.redelf.widget.fastscrolleralphabet.R;
import com.redelf.widget.fastscrolleralphabet.models.AlphabetItem;

import java.util.List;

public class AlphabetAdapter extends RecyclerView.Adapter<AlphabetAdapter.ViewHolder> {
    private Context mContext;
    private List<AlphabetItem> mDataArray;
    private OnItemClickListener listener;

    public AlphabetAdapter(Context context, List<AlphabetItem> dataSet) {
        this.mContext = context;
        this.mDataArray = dataSet;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void refreshDataChange(List<AlphabetItem> newDataSet) {
        this.mDataArray = newDataSet;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mDataArray == null)
            return 0;
        return mDataArray.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View alphabet = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alphabet_layout, parent, false);
        return new ViewHolder(alphabet);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(mDataArray.get(position), position);
    }

    public interface OnItemClickListener {
        void OnItemClicked(int alphabetPosition, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord;

        public ViewHolder(View itemView) {
            super(itemView);
            tvWord = (TextView) itemView.findViewById(R.id.tv_word);
        }

        public void bind(final AlphabetItem alphabetItem, final int position) {
            if (alphabetItem == null || alphabetItem.word == null)
                return;

            // Text
            tvWord.setText(alphabetItem.word);
            // Style
            tvWord.setTypeface(null, alphabetItem.isActive ? Typeface.BOLD : Typeface.NORMAL);
            // Text color
            tvWord.setTextColor(alphabetItem.isActive
                    ? mContext.getResources().getColor(R.color.alphabet_text_selected_color)
                    : mContext.getResources().getColor(R.color.alphabet_text_color));
            // Click event
            tvWord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener == null) {
                        return;
                    }

                    listener.OnItemClicked(alphabetItem.position, position);
                }
            });
        }
    }
}
