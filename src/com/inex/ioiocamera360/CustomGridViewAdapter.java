package com.inex.ioiocamera360;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomGridViewAdapter extends ArrayAdapter<Item> {
	Context context; 
	int layoutResourceId; 
	ArrayList<Item> data = new ArrayList<Item>();

	public CustomGridViewAdapter(Context context, int layoutResourceId, ArrayList<Item> data) {
		super(context, layoutResourceId, data); 
		this.layoutResourceId = layoutResourceId; 
		this.context = context; 
		this.data = data; 
	}
	
	public View getView(int position, View convertView, ViewGroup parent) { 
		View row = convertView; 
		RecordHolder holder = null;
		
		if (row == null) { 
			LayoutInflater inflater = ((Activity) context).getLayoutInflater(); 
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new RecordHolder(); 
			holder.txtTitle = (TextView) row.findViewById(R.id.tvFileName); 
			holder.imageItem = (ImageView) row.findViewById(R.id.ivThumbnail); 
			row.setTag(holder);
		} else { 
			holder = (RecordHolder) row.getTag(); 
		}
		
		Item item = data.get(position); 
		holder.txtTitle.setText(item.getTitle()); 
		holder.txtTitle.setTextSize(context.getResources().getInteger(R.integer.text_name_size));
		holder.imageItem.setImageBitmap(item.getImage()); 
		
		return row;
	}
	
	public class RecordHolder { 
		TextView txtTitle; 
		ImageView imageItem; 
	}
	
	public int dpToPx(int dp) {
	    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
	    int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));       
	    return px;
	}
}